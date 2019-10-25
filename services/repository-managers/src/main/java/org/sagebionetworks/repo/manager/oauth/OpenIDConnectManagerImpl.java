package org.sagebionetworks.repo.manager.oauth;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.StackEncrypter;
import org.sagebionetworks.repo.manager.oauth.claimprovider.OIDCClaimProvider;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.OAuthClientDao;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.securitytools.EncryptionUtils;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

public class OpenIDConnectManagerImpl implements OpenIDConnectManager {
	private static final long AUTHORIZATION_CODE_TIME_LIMIT_MILLIS = 60000L; // one minute

	// from https://openid.net/specs/openid-connect-core-1_0.html#ClaimsParameter
	private static final String ID_TOKEN_CLAIMS_KEY = "id_token";
	private static final String USER_INFO_CLAIMS_KEY = "userinfo";
	
	@Autowired
	private StackEncrypter stackEncrypter;

	@Autowired
	private OAuthClientDao oauthClientDao;

	@Autowired
	private AuthenticationDAO authDao;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;
	
	@Autowired
	Clock clock;
	
	/**
	 * Injected.
	 */
	private Map<OIDCClaimName, OIDCClaimProvider> claimProviders;
	
	public void setClaimProviders(Map<OIDCClaimName, OIDCClaimProvider> claimProviders) {
		this.claimProviders = claimProviders;
	}

	/*
	 * The scope parameter in an OAuth authorization request is a space-delimited list of scope values.
	 */
	public static List<OAuthScope> parseScopeString(String s) {
		if (StringUtils.isEmpty(s)) {
			return Collections.EMPTY_LIST;
		}
		String decoded;
		try {
			decoded = URLDecoder.decode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		List<OAuthScope> result = new ArrayList<OAuthScope>();
		for (String token : decoded.split("\\s")) {
			OAuthScope scope;
			try {
				scope = OAuthScope.valueOf(token);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Unrecognized scope: "+token, e);
			}
			result.add(scope);
		}
		return result;
	}

	public static void validateAuthenticationRequest(OIDCAuthorizationRequest authorizationRequest, OAuthClient client) {
		ValidateArgument.validUrl(authorizationRequest.getRedirectUri(), "Redirect URI");
		if (!client.getRedirect_uris().contains(authorizationRequest.getRedirectUri())) {
			throw new IllegalArgumentException("Redirect URI "+authorizationRequest.getRedirectUri()+
					" is not registered for "+client.getClient_name());
		}		
		if (OAuthResponseType.code!=authorizationRequest.getResponseType()) 
			throw new IllegalArgumentException("Unsupported response type "+authorizationRequest.getResponseType());
	}

	@Override
	public OIDCAuthorizationRequestDescription getAuthenticationRequestDescription(
			OIDCAuthorizationRequest authorizationRequest) {

		ValidateArgument.required(authorizationRequest, "Authorization Request");
		ValidateArgument.requiredNotEmpty(authorizationRequest.getClientId(), "Client ID");
		OAuthClient client;
		try {
			client = oauthClientDao.getOAuthClient(authorizationRequest.getClientId());
		} catch (NotFoundException e) {
			throw new IllegalArgumentException("Invalid OAuth Client ID: "+authorizationRequest.getClientId());
		}
		validateAuthenticationRequest(authorizationRequest, client);
		OIDCAuthorizationRequestDescription result = new OIDCAuthorizationRequestDescription();
		result.setClientId(client.getClient_id());
		result.setRedirect_uri(authorizationRequest.getRedirectUri());

		List<OAuthScope> scopes = parseScopeString(authorizationRequest.getScope());
		Set<String> scopeDescriptions = new TreeSet<String>();
		for (OAuthScope scope : scopes) {
			String scopeDescription = null;
			switch (scope) {
			case openid:
				// required for OIDC requests.  Doesn't add anything to what access is requested
				break;
			default:
				// unrecognized scope values should be ignored https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest
				break;
			}
			if (StringUtils.isNotEmpty(scopeDescription)) {
				scopeDescriptions.add(scopeDescription);
			}
		}

		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (scopes.contains(OAuthScope.openid) && !StringUtils.isEmpty(authorizationRequest.getClaims())) {
			scopeDescriptions.addAll(getDecriptionsForClaims(authorizationRequest.getClaims(), ID_TOKEN_CLAIMS_KEY));
			scopeDescriptions.addAll(getDecriptionsForClaims(authorizationRequest.getClaims(), USER_INFO_CLAIMS_KEY));
		}
		result.setScope(new ArrayList<String>(scopeDescriptions));
		return result;
	}
	
	private Set<String> getDecriptionsForClaims(String claimsJsonString, String jsonField) {
		Set<String> scopeDescriptions = new TreeSet<String>();
		{
			Map<OIDCClaimName,OIDCClaimsRequestDetails> idTokenClaimsMap = 
					ClaimsJsonUtil.getClaimsMapFromClaimsRequestParam(claimsJsonString, jsonField);
			for (OIDCClaimName claim : idTokenClaimsMap.keySet()) {
				OIDCClaimProvider provider = claimProviders.get(claim);
				if (provider!=null) {
					scopeDescriptions.add(provider.getDescription());
				}
			}
		}
		return scopeDescriptions;
	}

	@Override
	public OAuthAuthorizationResponse authorizeClient(UserInfo userInfo,
			OIDCAuthorizationRequest authorizationRequest) {
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new UnauthorizedException("Anonymous users may not provide access to OAuth clients.");
		}

		OAuthClient client;
		try {
			client = oauthClientDao.getOAuthClient(authorizationRequest.getClientId());
		} catch (NotFoundException e) {
			throw new IllegalArgumentException("Invalid OAuth Client ID: "+authorizationRequest.getClientId());
		}
		validateAuthenticationRequest(authorizationRequest, client);

		authorizationRequest.setUserId((new Long(userInfo.getId()).toString()));
		authorizationRequest.setAuthorizedAt(clock.now());
		authorizationRequest.setAuthenticatedAt(authDao.getSessionValidatedOn(userInfo.getId()));

		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		try {
			authorizationRequest.writeToJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
		String serializedAuthorizationRequest = adapter.toJSONString();
		String encryptedAuthorizationRequest = stackEncrypter.encryptAndBase64EncodeStringWithStackKey(serializedAuthorizationRequest);

		OAuthAuthorizationResponse result = new OAuthAuthorizationResponse();
		result.setAccess_code(encryptedAuthorizationRequest);
		return result;
	}

	// As per, https://openid.net/specs/openid-connect-core-1_0.html#PairwiseAlg
	public String ppid(String userId, String clientId) {
		String sectorIdentifierSecret = oauthClientDao.getSectorIdentifierSecretForClient(clientId);
		return EncryptionUtils.encrypt(userId, sectorIdentifierSecret);
	}

	public String getUserIdFromPPID(String ppid, String clientId) {
		String sectorIdentifierSecret = oauthClientDao.getSectorIdentifierSecretForClient(clientId);
		return EncryptionUtils.decrypt(ppid, sectorIdentifierSecret);
	}
	
	/*
	 * Given the scopes and additional OIDC claims requested by the user, return the 
	 * user info claims to add to the returned User Info object or JSON Web Token
	 */
	public Map<OIDCClaimName,Object> getUserInfo(final String userId, List<OAuthScope> scopes, Map<OIDCClaimName, OIDCClaimsRequestDetails>  oidcClaims) {
		Map<OIDCClaimName,Object> result = new HashMap<OIDCClaimName,Object>();
		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (!scopes.contains(OAuthScope.openid)) return result;

		for (OIDCClaimName claimName : oidcClaims.keySet()) {
			Object claimValue = null;
			OIDCClaimProvider claimProvider = claimProviders.get(claimName);
			if (claimProvider!=null) {
				claimValue = claimProvider.getClaim(userId, oidcClaims.get(claimName));
			}
			// from https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
			// "If a Claim is not returned, that Claim Name SHOULD be omitted from the JSON object 
			// representing the Claims; it SHOULD NOT be present with a null or empty string value."
			if (claimValue!=null) {
				result.put(claimName, claimValue);
			}
		}
		return result;
	}

	@Override
	public OIDCTokenResponse getAccessToken(String code, String verifiedClientId, String redirectUri, String oauthEndpoint) {
		ValidateArgument.required(code, "Authorization Code");
		ValidateArgument.required(verifiedClientId, "OAuth Client ID");
		ValidateArgument.required(redirectUri, "Redirect URI");
		ValidateArgument.required(oauthEndpoint, "Authorization Endpoint");
		String serializedAuthorizationRequest;
		try {
			serializedAuthorizationRequest = stackEncrypter.decryptStackEncryptedAndBase64EncodedString(code);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid authorization code: "+code, e);
		}
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		try {
			JSONObjectAdapter adapter = new JSONObjectAdapterImpl(serializedAuthorizationRequest);
			authorizationRequest.initializeFromJSONObject(adapter);
		} catch (JSONObjectAdapterException e) {
			throw new IllegalStateException("Incorrectly formatted authorization code: "+code, e);
		}

		// enforce expiration of authorization code
		long now = clock.currentTimeMillis();
		if (now > authorizationRequest.getAuthorizedAt().getTime()+AUTHORIZATION_CODE_TIME_LIMIT_MILLIS) {
			throw new IllegalArgumentException("Authorization code has expired.");
		}

		// ensure redirect URI matches
		if (!authorizationRequest.getRedirectUri().equals(redirectUri)) {
			throw new IllegalArgumentException("URI mismatch: "+authorizationRequest.getRedirectUri()+" vs. "+redirectUri);
		}

		List<OAuthScope> scopes = parseScopeString(authorizationRequest.getScope());

		Date authTime = null;
		if (authorizationRequest.getAuthenticatedAt()!=null) {
			authTime = authorizationRequest.getAuthenticatedAt();
		}

		// The following implements 'pairwise' subject_type, https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationResponse
		// Pairwise Pseudonymous Identifier (PPID)
		String ppid = ppid(authorizationRequest.getUserId(), verifiedClientId);
		String oauthClientId = authorizationRequest.getClientId();

		OIDCTokenResponse result = new OIDCTokenResponse();

		// Use of [the OpenID Connect] extension [to OAuth 2.0] is requested by Clients by including the openid scope value in the Authorization Request.
		// https://openid.net/specs/openid-connect-core-1_0.html#Introduction
		if (scopes.contains(OAuthScope.openid)) {
			String idTokenId = UUID.randomUUID().toString();
			Map<OIDCClaimName,Object> userInfo = getUserInfo(authorizationRequest.getUserId(), 
					scopes, ClaimsJsonUtil.getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), ID_TOKEN_CLAIMS_KEY));
			String idToken = oidcTokenHelper.createOIDCIdToken(oauthEndpoint, ppid, oauthClientId, now, 
					authorizationRequest.getNonce(), authTime, idTokenId, userInfo);
			result.setId_token(idToken);
		}

		String accessTokenId = UUID.randomUUID().toString();
		String accessToken = oidcTokenHelper.createOIDCaccessToken(oauthEndpoint, ppid, 
				oauthClientId, now, authTime, accessTokenId, scopes, 
				ClaimsJsonUtil.getClaimsMapFromClaimsRequestParam(authorizationRequest.getClaims(), USER_INFO_CLAIMS_KEY));
		result.setAccess_token(accessToken);
		return result;
	}
	
	@Override
	public Object getUserInfo(Jwt<JwsHeader,Claims> accessToken, String oauthEndpoint) {
		Claims accessTokenClaims = accessToken.getBody();
		// We set exactly one Audience when creating the token
		String oauthClientId = accessTokenClaims.getAudience();
		if (oauthClientId==null) {
			throw new IllegalArgumentException("Missing 'audience' value in the OAuth Access Token.");
		}
		OAuthClient oauthClient = oauthClientDao.getOAuthClient(oauthClientId);

		String ppid = accessTokenClaims.getSubject();
		
		// userId is used to retrieve the user info
		String userId = getUserIdFromPPID(ppid, oauthClientId);

		Date authTime = authDao.getSessionValidatedOn(Long.parseLong(userId));

		List<OAuthScope> scopes = ClaimsJsonUtil.getScopeFromClaims(accessTokenClaims);
		Map<OIDCClaimName, OIDCClaimsRequestDetails>  oidcClaims = ClaimsJsonUtil.getOIDCClaimsFromClaimSet(accessTokenClaims);

		Map<OIDCClaimName,Object> userInfo = getUserInfo(userId, scopes, oidcClaims);

		// From https://openid.net/specs/openid-connect-registration-1_0.html#ClientMetadata
		// "If [a signing algorithm] is specified, the response will be JWT serialized, and signed using JWS. 
		// The default, if omitted, is for the UserInfo Response to return the Claims as a UTF-8 
		// encoded JSON object using the application/json content-type."
		// 
		// Note: This leaves ambiguous what to do if the client is registered with a signing algorithm
		// and then sends a request with Accept: application/json or vice versa (registers with no 
		// algorithm and then sends a request with Accept: application/jwt).
		boolean returnJson = oauthClient.getUserinfo_signed_response_alg()==null;

		if (returnJson) {
			// https://openid.net/specs/openid-connect-core-1_0.html#UserInfoResponse
			userInfo.put(OIDCClaimName.sub, ppid);
			return userInfo;
		} else {
			String jwtIdToken = oidcTokenHelper.createOIDCIdToken(oauthEndpoint, ppid, oauthClientId, clock.currentTimeMillis(), null,
					authTime, UUID.randomUUID().toString(), userInfo);

			return new JWTWrapper(jwtIdToken);
		}
	}
}

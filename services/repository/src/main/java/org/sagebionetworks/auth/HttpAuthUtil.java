package org.sagebionetworks.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants;

import com.sun.jersey.core.util.Base64;

public class HttpAuthUtil {

	public static UserNameAndPassword getBasicAuthenticationCredentials(HttpServletRequest httpRequest) {
		String header = httpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);
		if (StringUtils.isBlank(header) || !header.startsWith(AuthorizationConstants.BASIC_PREFIX)) return null;

		String base64EncodedCredentials = header.substring(AuthorizationConstants.BASIC_PREFIX.length()).trim();
		String basicCredentials = Base64.base64Decode(base64EncodedCredentials);
		int colon = basicCredentials.indexOf(":");
		if (colon>0 && colon<basicCredentials.length()-1) {
			String name = basicCredentials.substring(0, colon);
			String password = basicCredentials.substring(colon+1);
			return new UserNameAndPassword(name, password);
		}
		return null;
	}

	public static String getBearerToken(HttpServletRequest httpRequest) {
		String header = httpRequest.getHeader(AuthorizationConstants.AUTHORIZATION_HEADER_NAME);
		return getBearerTokenFromAuthorizationHeader(header);
	}

	public static String getBearerTokenFromAuthorizationHeader(String header) {
		if (StringUtils.isBlank(header) || !header.startsWith(AuthorizationConstants.BEARER_TOKEN_HEADER)) return null;
		return header.substring(AuthorizationConstants.BEARER_TOKEN_HEADER.length()).trim();
	}
	
	/*
	 * Set the given bearerToken as an Authorization header, overwriting any other Authorization headers
	 */
	public static void setBearerTokenHeader(Map<String, String[]> headers, String bearerToken) {
		headers.put(AuthorizationConstants.AUTHORIZATION_HEADER_NAME, 
				new String[] {AuthorizationConstants.BEARER_TOKEN_HEADER+bearerToken});
	}
	
	private static final List<String> AUTHORIZATION_HEADERS_LOWER_CASE = 
			Arrays.asList(new String[] {
					AuthorizationConstants.AUTHORIZATION_HEADER_NAME.toLowerCase(),
					AuthorizationConstants.SESSION_TOKEN_PARAM.toLowerCase(),
					AuthorizationConstants.USER_ID_HEADER.toLowerCase(),
					AuthorizationConstants.SIGNATURE_TIMESTAMP.toLowerCase(),
					AuthorizationConstants.SIGNATURE.toLowerCase(),
					AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER.toLowerCase()
			});
	
	/*
	 * Get all the request headers *except* the authorization headers used by Synapse
	 */
	public static Map<String, String[]> filterAuthorizationHeaders(HttpServletRequest request) {
		Map<String, String[]> result = new HashMap<String, String[]> ();
		for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
			String headerName = e.nextElement();
			if (AUTHORIZATION_HEADERS_LOWER_CASE.contains(headerName.toLowerCase())) {
				continue;
			}
			List<String> headerValues = new ArrayList<String>();
			for (Enumeration<String> n = request.getHeaders(headerName); n.hasMoreElements();) {
				String headerValue = n.nextElement();
				headerValues.add(headerValue);
			}
			result.put(headerName, headerValues.toArray(new String[headerValues.size()]));
		}
		return result;
	}
}

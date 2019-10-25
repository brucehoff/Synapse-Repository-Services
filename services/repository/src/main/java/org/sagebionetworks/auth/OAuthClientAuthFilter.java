package org.sagebionetworks.auth;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.repo.manager.oauth.OAuthClientManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.springframework.beans.factory.annotation.Autowired;

public class OAuthClientAuthFilter implements Filter {
	
	@Autowired
	private OAuthClientManager oauthClientManager;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest)request;

		UserNameAndPassword up = HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		
		boolean validCredentials=false;
		String oauthClientId=null;

		if (up!=null) {
			oauthClientId = up.getUserName();
			OAuthClientIdAndSecret clientCreds = new OAuthClientIdAndSecret();
			clientCreds.setClient_id(oauthClientId);
			clientCreds.setClient_secret(up.getPassword());
			validCredentials = oauthClientManager.validateClientCredentials(clientCreds);
		}
		
		if (validCredentials) {
			// get the current headers, but be sure to leave behind anything that might be mistaken for a valid
			// authentication header 'down the filter chain'
			Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(httpRequest);
			modHeaders.put(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER, new String[] {oauthClientId});
			HttpServletRequest modRqst = new ModHttpServletRequest(httpRequest, modHeaders, null);
			chain.doFilter(modRqst, response);
		} else {
			HttpServletResponse httpResponse = (HttpServletResponse)response;
			httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			httpResponse.setContentType("application/json");
			httpResponse.getOutputStream().println("{\"reason\":\"Missing or invalid OAuth 2.0 client credentials\"}");
			httpResponse.getOutputStream().flush();
		}
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Nothing to do
	}

	@Override
	public void destroy() {
		// Nothing to do
	}

}

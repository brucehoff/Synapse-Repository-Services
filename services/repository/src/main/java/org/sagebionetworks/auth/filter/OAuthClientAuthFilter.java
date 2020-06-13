package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.repo.manager.oauth.OAuthClientManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("oauthClientAuthFilter")
public class OAuthClientAuthFilter implements Filter {
	private Log logger = LogFactory.getLog(getClass());
	

	private static final String INVALID_CREDENTIAL_MSG = "OAuth Client ID and secret must be passed via Basic Authentication. Credentials are missing or invalid.";

	private OAuthClientManager oauthClientManager;
	
	private StackConfiguration config;
	private Consumer consumer;

	@Autowired
	public OAuthClientAuthFilter(StackConfiguration config, Consumer consumer, OAuthClientManager oauthClientManager) {
		this.oauthClientManager = oauthClientManager;
		this.config=config;
		this.consumer=consumer;
		
		System.out.println("OAuthClientAuthFilter constructor: this: "+this+
				" oauthClientManager: "+this.oauthClientManager+
				" config: "+this.config+
				" consumer: "+this.consumer);
	}


	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		System.out.println("OAuthClientAuthFilter.doFilter: this: "+this+
				" oauthClientManager: "+this.oauthClientManager+
				" config: "+this.config+
				" consumer: "+this.consumer);

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("Only HTTP requests are supported");
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		Optional<UserNameAndPassword> credentials;

		try {
			credentials = HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		} catch (IllegalArgumentException e) {
			(new FilterHelper(config, consumer)).rejectRequest(true, httpResponse, e);
			return;
		}

		if (!credentials.isPresent()) {
			(new FilterHelper(config, consumer)).rejectRequest(true, httpResponse, INVALID_CREDENTIAL_MSG);
			return;
		}

		validateCredentialsAndDoFilterInternal(httpRequest, httpResponse, filterChain, credentials);
	}
		
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void destroy() {

	}


	private void validateCredentialsAndDoFilterInternal(
			HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
			FilterChain filterChain, Optional<UserNameAndPassword> credentials) throws IOException, ServletException {

		if (credentials.isPresent() && !validCredentials(credentials.get())) {
			(new FilterHelper(config, consumer)).rejectRequest(true, httpResponse, INVALID_CREDENTIAL_MSG);
			return;
		}

		doFilterInternal(httpRequest, httpResponse, filterChain, credentials.orElse(null));
	}

	private boolean validCredentials(UserNameAndPassword credentials) {
		OAuthClientIdAndSecret clientCreds = new OAuthClientIdAndSecret();

		clientCreds.setClient_id(credentials.getUserName());
		clientCreds.setClient_secret(credentials.getPassword());

		return oauthClientManager.validateClientCredentials(clientCreds);
	}

	private void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, UserNameAndPassword credentials) throws ServletException, IOException {
		
		if (credentials == null) {
			throw new IllegalStateException("Credentials were expected but not supplied");
		}

		String oauthClientId = credentials.getUserName();

		// get the current headers, but be sure to leave behind anything that might be
		// mistaken for a valid
		// authentication header 'down the filter chain'

		Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(request);
		modHeaders.put(AuthorizationConstants.OAUTH_VERIFIED_CLIENT_ID_HEADER, new String[] { oauthClientId });
		HttpServletRequest modRqst = new ModHttpServletRequest(request, modHeaders, null);

		filterChain.doFilter(modRqst, response);
	}

}

package org.sagebionetworks.auth.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.auth.HttpAuthUtil;
import org.sagebionetworks.auth.UserNameAndPassword;
import org.sagebionetworks.auth.services.AuthenticationService;
import org.sagebionetworks.authutil.ModHttpServletRequest;
import org.sagebionetworks.cloudwatch.Consumer;
import org.sagebionetworks.cloudwatch.MetricUtils;
import org.sagebionetworks.cloudwatch.ProfileData;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudwatch.model.StandardUnit;

@Component("dockerClientAuthFilter")
public class DockerClientAuthFilter implements Filter {	
	private AuthenticationService authenticationService;
	
	private OIDCTokenHelper oidcTokenHelper;

	private OpenIDConnectManager oidcManager;
	
	@Autowired
	public DockerClientAuthFilter(
			StackConfiguration config, 
			Consumer consumer, 
			AuthenticationService authenticationService,
			OIDCTokenHelper oidcTokenHelper,
			OpenIDConnectManager oidcManager) {
			this.config = config;
			this.consumer = consumer;
		this.authenticationService = authenticationService;
		this.oidcTokenHelper=oidcTokenHelper;
		this.oidcManager=oidcManager;
	}

	// The anonymous user can come in
	private boolean credentialsRequired() {
		return false;
	}
	
	private boolean reportBadCredentialsMetric() {
		return true;
	}
	
	private void validateCredentialsAndDoFilterInternal(
			HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
			FilterChain filterChain, Optional<UserNameAndPassword> credentials) throws IOException, ServletException {
		
		Optional<UserIdAndAccessToken> userIdAndAccessToken = Optional.empty();
		
		if (credentials.isPresent()) {
			userIdAndAccessToken = getUserIdAndAccessToken(credentials.get());
			if (!userIdAndAccessToken.isPresent()) {
				rejectRequest(httpResponse, getInvalidCredentialsMessage());
				return;
			}
		}

		doFilterInternal(httpRequest, httpResponse, filterChain, userIdAndAccessToken);
	}

	/*
	 * return the user id and access token for the given credentials or nothing if credentials are invalid
	 */
	private Optional<UserIdAndAccessToken> getUserIdAndAccessToken(UserNameAndPassword credentials) {
		try {
			// is the password actually an access token?
			String userId = oidcManager.getUserId(credentials.getPassword());
			return Optional.of(new UserIdAndAccessToken(userId, credentials.getPassword()));
		} catch (IllegalArgumentException iae) {
			// the password is NOT a (valid) access token,
			// but maybe it's a password
			LoginRequest credential = new LoginRequest();
			
			credential.setUsername(credentials.getUserName());
			credential.setPassword(credentials.getPassword());
			
			try {
				authenticationService.login(credential);
			} catch (UnauthenticatedException e) {
				return Optional.empty();
			}
			PrincipalAlias alias = null;
			try {
				String username = credentials.getUserName();
				alias = authenticationService.lookupUserForAuthentication(username);
			} catch (NotFoundException e) {
				return Optional.empty();
			}
			Long userId = alias.getPrincipalId();
			String accessToken = oidcTokenHelper.createTotalAccessToken(userId);
			return Optional.of(new UserIdAndAccessToken(userId.toString(), accessToken));
		}
	}
	
	/*
	 * userIdAndAccessToken is empty for anonymous requests
	 */
	private void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, Optional<UserIdAndAccessToken> userIdAndAccessToken) throws ServletException, IOException {
		
		String userId = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString();
		
		Map<String, String[]> modHeaders = HttpAuthUtil.filterAuthorizationHeaders(request);
		
		if (userIdAndAccessToken.isPresent()) {
			userId = userIdAndAccessToken.get().getUserId();
			String accessToken = userIdAndAccessToken.get().getAccessToken();
			HttpAuthUtil.setBearerTokenHeader(modHeaders, accessToken);
		}
		
		Map<String, String[]> modParams = new HashMap<String, String[]>(request.getParameterMap());
		modParams.put(AuthorizationConstants.USER_ID_PARAM, new String[] { userId });
		HttpServletRequest modRqst = new ModHttpServletRequest(request, modHeaders, modParams);
		
		filterChain.doFilter(modRqst, response);
	}
	
	

	private static final String MISSING_CREDENTIALS_MSG = "Missing required credentials in the authorization header.";
	private static final String INVALID_CREDENTIALS_MSG = "Invalid credentials.";
	private static final String CLOUD_WATCH_NAMESPACE_PREFIX = "Authentication";
	private static final String CLOUD_WATCH_METRIC_NAME = "BadCredentials";
	private static final String CLOUD_WATCH_DIMENSION_FILTER = "filterClass";
	private static final String CLOUD_WATCH_DIMENSION_MESSAGE = "message";
	private static final String CLOUD_WATCH_UNIT_COUNT = StandardUnit.Count.toString();

	private Log logger = LogFactory.getLog(getClass());
	
	private StackConfiguration config;
	private Consumer consumer;
	
	@Override
	public final void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {

		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("Only HTTP requests are supported");
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		Optional<UserNameAndPassword> credentials;

		try {
			credentials = HttpAuthUtil.getBasicAuthenticationCredentials(httpRequest);
		} catch (IllegalArgumentException e) {
			rejectRequest(httpResponse, e);
			return;
		}

		if (credentialsRequired() && !credentials.isPresent()) {
			rejectRequest(httpResponse, MISSING_CREDENTIALS_MSG);
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
	
	/**
	 * Rejects the http request due to the given exception sending a 401 in the response with the exception message.
	 * If {@link #reportBadCredentialsMetric()} is true sends a bad credentials metric to cloud watch
	 * 
	 * @param response
	 * @param ex
	 * @throws IOException
	 */
	protected void rejectRequest(HttpServletResponse response, Exception ex) throws IOException {
				
		if (reportBadCredentialsMetric()) {
			
			logger.error(ex.getMessage(), ex);
			
			// We log in cloudwatch the stack trace of the exception
			String stackTraceString = MetricUtils.stackTracetoString(ex);
			sendBadCredentialMetric(consumer, getClass().getName(), config.getStackInstance(), stackTraceString);
		}
		
		HttpAuthUtil.reject(response, ex.getMessage());
	}

	/**
	 * Rejects a the http request and sends a 401 in the response with the given
	 * message as the reason, if {@link #reportBadCredentialsMetric()} is true sends
	 * a bad credentials metric to cloud watch
	 * 
	 * @param response
	 * @param message The message to be returned in the response
	 * @throws IOException
	 */
	protected void rejectRequest(HttpServletResponse response, String message) throws IOException {
		if (reportBadCredentialsMetric()) {
			sendBadCredentialMetric(consumer, getClass().getName(), config.getStackInstance(), message);
		}

		HttpAuthUtil.reject(response, message);
	}

	/**
	 * @return The message returned if the credentials are invalid
	 */
	protected String getInvalidCredentialsMessage() {
		return INVALID_CREDENTIALS_MSG;
	}

	private static void sendBadCredentialMetric(Consumer consumer, String filterClass, String stackInstance, String message) {
		
		Date timestamp = new Date();
		
		List<ProfileData> data = new ArrayList<>();
		
		// Note: Setting dimensions defines a new metric since the metric itself is identified by the name and dimensions
		// (See https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#Dimension)
		// 
		// We send two different metrics (with the same timestamp) one that includes the message so that we can quickly inspect it
		// and one without the message so that an alarm can be created (since we don't know the message in advance it would be impossible
		// to create an alarm).
		
		data.add(generateProfileData(timestamp, filterClass, stackInstance, null));
		
		if (!StringUtils.isBlank(message)) {
			data.add(generateProfileData(timestamp, filterClass, stackInstance, message));
		}
		
		consumer.addProfileData(data);
	}
	
	private static ProfileData generateProfileData(Date timestamp, String filterClass, String stackInstance, String message) {
		ProfileData logEvent = new ProfileData();

		logEvent.setNamespace(String.format("%s - %s", CLOUD_WATCH_NAMESPACE_PREFIX, stackInstance));
		logEvent.setName(CLOUD_WATCH_METRIC_NAME);
		logEvent.setValue(1.0);
		logEvent.setUnit(CLOUD_WATCH_UNIT_COUNT);
		logEvent.setTimestamp(timestamp);
		
		Map<String, String> dimensions = new HashMap<>();
		
		dimensions.put(CLOUD_WATCH_DIMENSION_FILTER, filterClass);
		
		if (message != null) {
			dimensions.put(CLOUD_WATCH_DIMENSION_MESSAGE, message);
		};
		
		logEvent.setDimension(dimensions);
		
		return logEvent;
	}

}

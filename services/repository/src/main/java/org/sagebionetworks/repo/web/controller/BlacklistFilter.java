package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.utils.DefaultHttpClientSingleton;
import org.sagebionetworks.utils.HttpClientHelper;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.http.HttpStatus;

public class BlacklistFilter implements Filter {
	
	// TODO make this a configuration parameter
	private static final String VERSIONS_SERVICE_ENDPOINT = "http://versions.synapse.sagebase.org/";
	
	private static void reject(int status, String reason, HttpServletResponse resp) throws IOException {
		resp.setStatus(403);
		resp.getWriter().println("{\"reason\", \""+reason+"\"}");		
	}

	private static Map<String,JSONObject> blackListCache = null; // maps client-type to blacklist info
	private static String BLACKLIST_FILTER_CACHE_TIMEOUT_MILLIS = "BLACKLIST_FILTER_CACHE_TIMEOUT_MILLIS";
	private static long BLACKLIST_FILTER_CACHE_TIMEOUT_DEFAULT = 60*1000L;
	
	private static Long cacheTimeout = null;
	private static Date lastCacheDump = null;
	
	@Override
	public void init(FilterConfig config) throws ServletException {
		initCaches();
	}
	
	private void initCaches() {
		blackListCache = Collections.synchronizedMap(new HashMap<String,JSONObject>());
		lastCacheDump = new Date();
		String s = System.getProperty(BLACKLIST_FILTER_CACHE_TIMEOUT_MILLIS);
		if (s!=null && s.length()>0) {
			cacheTimeout = Long.parseLong(s);
		} else {
			cacheTimeout = BLACKLIST_FILTER_CACHE_TIMEOUT_DEFAULT;
		}
	}
	
	private void checkCacheDump() {
		Date now = new Date();
		if (lastCacheDump.getTime()+cacheTimeout<now.getTime()) {
			blackListCache.clear();
			lastCacheDump = now;
		}
	}

	

	// TODO: factor out logic so it's testable
	@Override
	public void doFilter(ServletRequest servletRqst, ServletResponse servletResp,
			FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest)servletRqst;
		HttpServletResponse resp = (HttpServletResponse)servletResp;

		boolean isBlackListed = false;
		String userAgent = req.getHeader("User-Agent");
		int slash = (userAgent==null ? -1 : userAgent.indexOf("/"));
		String reason = null;
		if (slash>0) {
			String clientType = userAgent.substring(0, slash);
			String clientVersion = userAgent.substring(slash+1);
			if (clientVersion.length()>0) {
				try {
					JSONObject versionInfo = null;
					checkCacheDump();
					if (blackListCache.containsKey(clientType)) {
						versionInfo = blackListCache.get(clientType);
					} else {
						// GET versions.synapse.sagebase.org/<clientType>
						// if URL is not found it means the client type is unrecognized, no black list
						versionInfo = getURLasJson(VERSIONS_SERVICE_ENDPOINT+clientType);
						blackListCache.put(clientType, versionInfo);
					}
					if (versionInfo!=null) {
						JSONArray blackList = versionInfo.getJSONArray("blacklist");
						// see if <clientVersion> is in black list.  if so, isBlackListed = True
						for (int i=0; clientVersion!=null && blackList!=null && i<blackList.length(); i++) {
							if (clientVersion.equals(blackList.getString(i))) isBlackListed=true;
						}
						String latestVersion = versionInfo.getString("latestVersion");
						String message = versionInfo.getString("message");
						reason ="This version, "+clientVersion+", of "+clientType+
							" has been disabled.  Please update to the latest version, "+
							latestVersion+(message==null?"":" \n"+message);
					}
				} catch (IOException e) {
					reject(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), resp);
					return;
				} catch (JSONException e) {
					reject(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), resp);
					return;
				}
			}
		}
		if (isBlackListed) {
			reject(HttpStatus.FORBIDDEN.value(), reason, resp);
		} else {
			filterChain.doFilter(servletRqst, servletResp);
		}
	}

	/**
	 * 
	 * @param requestURL
	 * @return if URL doesn't exist, returns null rather than throwing an exception
	 * @throws IOException
	 * @throws JSONException
	 */
	public static JSONObject getURLasJson(String requestURL) throws IOException, JSONException {
			HttpResponse response = null;
			try {
			    response = HttpClientHelper.performRequest(DefaultHttpClientSingleton.getInstance(), requestURL,
					"GET", "", null);
			    int status = response.getStatusLine().getStatusCode();
			    if (HttpStatus.NOT_FOUND.value()==status) {
			    	return null;
			    }
			    if(HttpStatus.OK.value() != status) {
					throw new IOException("Unable to retrieve "+requestURL);			    	
			    }
			    // TODO move CrowdAuthUtil.readInputStream to a common library
				byte[] respBody = (CrowdAuthUtil.readInputStream(response.getEntity().getContent())).getBytes();
				return new JSONObject(new String(respBody));
			} catch (HttpClientHelperException hche) {
				throw new IOException("Unable to retrieve "+requestURL);			    	
			} 
	}
	
	@Override
	public void destroy() {
	}


}

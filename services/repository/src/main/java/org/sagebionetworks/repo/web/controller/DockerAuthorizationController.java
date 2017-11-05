package org.sagebionetworks.repo.web.controller;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.trash.EntityInTrashCanException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthenticatedException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.docker.*;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 
 * These services allow Synapse to act as an authorization service for a Docker Registry.
 * For more details see: https://github.com/docker/distribution/blob/master/docs/spec/auth/token.md
 * 
 *
 */
@ControllerInfo(displayName="Docker Authorization Services", path="docker/v1")
@Controller
@RequestMapping(UrlHelpers.DOCKER_PATH)
public class DockerAuthorizationController {
	private static Logger log = LogManager.getLogger(DockerAuthorizationController.class);

	
	@Autowired
	private ServiceProvider serviceProvider;
	
	/**
	 * Authorize Docker operation.  This service is called by the Docker client only and is not for general use.
	 * @param userId
	 * @param service
	 * @param scope
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DOCKER_AUTHORIZATION, method = RequestMethod.GET)
	public @ResponseBody
	DockerAuthorizationToken authorizeDockerAccess(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = AuthorizationConstants.DOCKER_SERVICE_PARAM, required=true) String service,
			@RequestParam(value = AuthorizationConstants.DOCKER_SCOPE_PARAM, required=false) List<String> scopes
			) throws NotFoundException {
		return serviceProvider.getDockerService().authorizeDockerAccess(userId, service, scopes);
	}
	
	@ExceptionHandler({
		UnauthorizedException.class, 
		NotFoundException.class, 
		IllegalArgumentException.class,
		InvalidModelException.class,
		EntityInTrashCanException.class})
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public @ResponseBody
	DockerErrorResponseList handleUnauthorizedException(UnauthorizedException ex,
			HttpServletRequest request) {
		return handleException(ex, request, true);
	}
	
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public @ResponseBody
	DockerErrorResponseList handleAllOtherExceptions(Exception ex,
			HttpServletRequest request) {
		log.error("Consider specifically handling exceptions of type "
						+ ex.getClass().getName());
		return handleException(ex, request, true);
	}

	private DockerErrorResponseList handleException(Throwable ex, HttpServletRequest request, boolean fullTrace) {
		// Always log the stack trace on develop stacks
		if (fullTrace || StackConfiguration.isDevelopStack()) {
			// Print the full stack trace
			log.error("Handling " + request.toString(), ex);
		} else {
			// Only print one line
			log.error("Handling " + request.toString());
		}

		String message = ex.getMessage()==null ? ex.getClass().getName() : ex.getMessage();
		DockerErrorResponseList erl = new DockerErrorResponseList();
		DockerErrorResponse er = new DockerErrorResponse();
		erl.setErrors(Collections.singletonList(er));
		er.setCode(DockerErrorCode.DENIED);
		er.setMessage(message);
		return erl;
	}

}

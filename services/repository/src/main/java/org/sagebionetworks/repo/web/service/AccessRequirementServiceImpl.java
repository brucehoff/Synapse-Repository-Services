package org.sagebionetworks.repo.web.service;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ControllerEntityClassHelper;
import org.sagebionetworks.repo.web.controller.ObjectTypeSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

public class AccessRequirementServiceImpl implements AccessRequirementService {

	@Autowired
	AccessRequirementManager accessRequirementManager;
	
	@Autowired
	UserManager userManager;

	@Autowired
	ObjectTypeSerializer objectTypeSerializer;

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessRequirementService#createAccessRequirement(java.lang.String, org.springframework.http.HttpHeaders, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public AccessRequirement createAccessRequirement(String userId, 
			HttpHeaders header, HttpServletRequest request) throws Exception {
		UserInfo userInfo = userManager.getUserInfo(userId);
		AccessRequirement accessRequirement = (AccessRequirement)ControllerEntityClassHelper.deserialize(request, header);
		return accessRequirementManager.createAccessRequirement(userInfo, accessRequirement);
	}
	
	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessRequirementService#getUnfulfilledAccessRequirement(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public PaginatedResults<AccessRequirement> getUnfulfilledAccessRequirement(
				String userId, String entityId,	HttpServletRequest request) 
				throws DatastoreException, UnauthorizedException, 
				NotFoundException, ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		QueryResults<AccessRequirement> results = 
			accessRequirementManager.getUnmetAccessRequirements(userInfo, entityId);
		
		return new PaginatedResults<AccessRequirement>(
				request.getServletPath()+UrlHelpers.ACCESS_REQUIREMENT_UNFULFILLED_WITH_ID, 
				results.getResults(),
				(int)results.getTotalNumberOfResults(), 
				1, 
				(int)results.getTotalNumberOfResults(),
				"", 
				false);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessRequirementService#getAccessRequirements(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public PaginatedResults<AccessRequirement> getAccessRequirements(
			String userId, String entityId,	HttpServletRequest request) 
			throws DatastoreException, UnauthorizedException, NotFoundException, 
			ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		QueryResults<AccessRequirement> results = 
			accessRequirementManager.getAccessRequirementsForEntity(userInfo, entityId);
		
		return new PaginatedResults<AccessRequirement>(
				request.getServletPath()+UrlHelpers.ACCESS_REQUIREMENT, 
				results.getResults(),
				(int)results.getTotalNumberOfResults(), 
				1, 
				(int)results.getTotalNumberOfResults(),
				"", 
				false);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.web.service.AccessRequirementService#deleteAccessRequirements(java.lang.String, java.lang.String)
	 */
	@Override
	public void deleteAccessRequirements(String userId, String requirementId) 
			throws DatastoreException, UnauthorizedException, NotFoundException, 
			ForbiddenException {
		UserInfo userInfo = userManager.getUserInfo(userId);

		accessRequirementManager.deleteAccessRequirement(userInfo, requirementId);

	}
	
}

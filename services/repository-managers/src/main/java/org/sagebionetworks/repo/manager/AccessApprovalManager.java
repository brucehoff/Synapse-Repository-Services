package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoRequest;
import org.sagebionetworks.repo.model.BatchAccessApprovalInfoResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRequest;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessorGroupRevokeRequest;
import org.sagebionetworks.repo.web.NotFoundException;

public interface AccessApprovalManager {
	
	/**
	 *  create access approval
	 */
	public AccessApproval createAccessApproval(UserAuthorization userAuthorization, AccessApproval accessApproval) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
	
	/**
	 * 
	 * @param userInfo
	 * @param approvalId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AccessApproval getAccessApproval(UserAuthorization userAuthorization, String approvalId) throws DatastoreException, NotFoundException;

	/**
	 *  get all the access approvals for an entity
	 */
	public List<AccessApproval> getAccessApprovalsForSubject(UserAuthorization userAuthorization, RestrictableObjectDescriptor subjectId, Long limit, Long offset) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/*
	 *  delete an access approval
	 */
	public void deleteAccessApproval(UserAuthorization userAuthorization, String AccessApprovalId) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Delete all access approvals that gives accessorId access to subject(s) that requires access requirement accessRequirementId
	 * 
	 * @param userInfo - the user who making the request
	 * @param accessRequirementId - the target access requirement
	 * @param accessorId - the user whose access is being revoked
	 * @throws UnauthorizedException - if the user is not an admin or an ACT member
	 */
	public void revokeAccessApprovals(UserAuthorization userAuthorization, String accessRequirementId, String accessorId) throws UnauthorizedException;

	/**
	 * List a page of accessor groups.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public AccessorGroupResponse listAccessorGroup(UserAuthorization userAuthorization, AccessorGroupRequest request);

	/**
	 * Revoke a group of accessors
	 * 
	 * @param userInfo
	 * @param request
	 */
	public void revokeGroup(UserAuthorization userAuthorization, AccessorGroupRevokeRequest request);

	/**
	 * Retrieve a batch of AccessApprovalInfo.
	 * 
	 * @param userInfo
	 * @param request
	 * @return
	 */
	public BatchAccessApprovalInfoResponse getAccessApprovalInfo(UserAuthorization userAuthorization, BatchAccessApprovalInfoRequest request);
}

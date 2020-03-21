package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.model.docker.RegistryEventAction.pull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.evaluation.manager.EvaluationPermissionsManager;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationStatus;
import org.sagebionetworks.repo.manager.form.FormManager;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.HasAccessorRequirement;
import org.sagebionetworks.repo.model.InviteeVerificationSignedToken;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAO;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.model.util.DockerNameUtil;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class AuthorizationManagerImpl implements AuthorizationManager {

	private static final String REPOSITORY_TYPE = "repository";
	private static final String REGISTRY_TYPE = "registry";
	private static final String REGISTRY_CATALOG = "catalog";	
	public static final Long TRASH_FOLDER_ID = Long.parseLong(
			StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	private static final String FILE_HANDLE_UNAUTHORIZED_TEMPLATE = "Only the creator of a FileHandle can access it directly by its ID.  FileHandleId = '%1$s', UserId = '%2$s'";
	public static final String ANONYMOUS_ACCESS_DENIED_REASON = "Anonymous cannot perform this action. Please login and try again.";
	private static final String FILE_HANDLE_ID_IS_NOT_ASSOCIATED_TEMPLATE = "FileHandleId: %1s is not associated with objectId: %2s of type: %3s";


	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private AccessRequirementDAO  accessRequirementDAO;
	@Autowired
	private ActivityDAO activityDAO;
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private VerificationDAO verificationDao;
	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private FileHandleAssociationManager fileHandleAssociationSwitch;
	@Autowired
	private V2WikiPageDao wikiPageDaoV2;
	@Autowired
	private org.sagebionetworks.repo.model.evaluation.SubmissionDAO submissionDAO;
	@Autowired
	private MessageManager messageManager;
	@Autowired
	private org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO dataAccessSubmissionDao;
	@Autowired
	private DockerNodeDao dockerNodeDao;
	@Autowired
	private GroupMembersDAO groupMembersDao;
	@Autowired
	private TokenGenerator tokenGenerator;
	@Autowired
	private FormManager formManager;
	
	@Override
	public AuthorizationStatus canAccess(UserAuthorization userAuthorization, String objectId, ObjectType objectType, ACCESS_TYPE accessType)
			throws DatastoreException, NotFoundException {
		switch (objectType) {
			case ENTITY:
				return entityPermissionsManager.hasAccess(objectId, accessType, userAuthorization);
			case EVALUATION:
				return evaluationPermissionsManager.hasAccess(userAuthorization, objectId, accessType);
			case ACCESS_REQUIREMENT:
				if (isACTTeamMemberOrAdmin(userAuthorization.getUserInfo())) { // TODO check granted scope
					return AuthorizationStatus.authorized();
				}
				if (accessType==ACCESS_TYPE.READ || accessType==ACCESS_TYPE.DOWNLOAD) {
					return AuthorizationStatus.authorized();
				}
				return AuthorizationStatus.accessDenied("Only ACT member can perform this action.");
			case ACCESS_APPROVAL:
				if (isACTTeamMemberOrAdmin(userAuthorization.getUserInfo())) { // TODO check granted scope
					return AuthorizationStatus.authorized();
				}
				return AuthorizationStatus.accessDenied("Only ACT member can perform this action.");
			case TEAM:
				if (userAuthorization.getUserInfo().isAdmin()) { // TODO check granted
					return AuthorizationStatus.authorized();
				}
				// everyone should be able to download the Team's Icon, even anonymous.
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					return AuthorizationStatus.authorized();
				}
				
				// just check the acl
				// TODO check scope
				boolean teamAccessPermission = aclDAO.canAccess(userAuthorization.getUserInfo().getGroups(), objectId, objectType, accessType);
				if (teamAccessPermission) {
					return AuthorizationStatus.authorized();
				} else {
					return AuthorizationStatus.accessDenied("Unauthorized to access Team "+objectId+" for "+accessType);
				}
			case VERIFICATION_SUBMISSION:
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					if (isACTTeamMemberOrAdmin(userAuthorization.getUserInfo()) ||// TODO check granted scope
							verificationDao.getVerificationSubmitter(Long.parseLong(objectId))==userAuthorization.getUserInfo().getId()) {
						return AuthorizationStatus.authorized();
					} else {
					return AuthorizationStatus.accessDenied(
							"You must be an ACT member or the owner of the Verification Submission to download its attachments.");
					}
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			case WIKI:{
				ACCESS_TYPE ownerAccessType = accessType;
				if(ACCESS_TYPE.DOWNLOAD == accessType){
					// Wiki download is checked against owner read.
					ownerAccessType = ACCESS_TYPE.READ;
				}
				WikiPageKey key = wikiPageDaoV2.lookupWikiKey(objectId);
				// check against the wiki owner
				return canAccess(userAuthorization, key.getOwnerObjectId(), key.getOwnerObjectType(), ownerAccessType);
			}
			case USER_PROFILE: {  
				// everyone should be able to download userProfile picture, even anonymous.
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					return AuthorizationStatus.authorized();
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			}
			case EVALUATION_SUBMISSIONS:
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					Submission submission = submissionDAO.get(objectId);
					return evaluationPermissionsManager.hasAccess(userAuthorization, submission.getEvaluationId(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION);
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			case MESSAGE: {
				if (accessType==ACCESS_TYPE.DOWNLOAD) {  // TODO check scope
					try {
						// if the user can get the message metadata, he/she can download the message
						messageManager.getMessage(userAuthorization.getUserInfo(), objectId);
						return AuthorizationStatus.authorized();
					} catch (UnauthorizedException e) {
						return AuthorizationStatus.accessDenied(e);
					}
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			}
			case DATA_ACCESS_REQUEST:
			case DATA_ACCESS_SUBMISSION: {
				if (accessType==ACCESS_TYPE.DOWNLOAD) { // TODO check scope
					if (isACTTeamMemberOrAdmin(userAuthorization.getUserInfo())) {
						return AuthorizationStatus.authorized();
					} else {
						return AuthorizationStatus.accessDenied("Download not allowed");
					}
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			}
			case FORM_DATA: { // TODO check scope
				if (accessType==ACCESS_TYPE.DOWNLOAD) {
					return formManager.canUserDownloadFormData(userAuthorization.getUserInfo(), objectId);
				} else {
					return AuthorizationStatus.accessDenied("Unexpected access type "+accessType);
				}
			}
			default:
				throw new IllegalArgumentException("Unknown ObjectType: "+objectType);
		}
	}

	@Override
	public AuthorizationStatus canCreate(UserAuthorization userAuthorization, String parentId, EntityType nodeType) 
		throws NotFoundException, DatastoreException {
		return entityPermissionsManager.canCreate(parentId, nodeType, userAuthorization);
	}

	@Override
	public AuthorizationStatus canChangeSettings(UserAuthorization userAuthorization, Node node) throws NotFoundException, DatastoreException {
		return entityPermissionsManager.canChangeSettings(node, userAuthorization);
	}

	@Override
	public AuthorizationStatus canAccessActivity(UserAuthorization userAuthorization, String activityId) throws DatastoreException, NotFoundException {
		if(userAuthorization.getUserInfo().isAdmin()) return AuthorizationStatus.authorized(); // TODO check scope
		
		// check if owner
		Activity act = activityDAO.get(activityId);
		if(act.getCreatedBy().equals(userAuthorization.getUserInfo().getId().toString()))// TODO check scope
				return AuthorizationStatus.authorized();
		
		// check if user has read access to any in result set (could be empty)
		int limit = 1000;
		int offset = 0;
		long remaining = 1; // just to get things started
		while(remaining > 0) {			
			PaginatedResults<Reference> generatedBy = activityDAO.getEntitiesGeneratedBy(activityId, limit, offset);
			remaining = generatedBy.getTotalNumberOfResults() - (offset+limit);
			for(Reference ref : generatedBy.getResults()) {
				String nodeId = ref.getTargetId();
				try {
					if(canAccess(userAuthorization, nodeId, ObjectType. ENTITY, ACCESS_TYPE.READ).isAuthorized()) {
						return AuthorizationStatus.authorized();
					}
				} catch (Exception e) {
					// do nothing, same as false
				}
			}
			offset += limit; 
		}
		// no access found to generated entities, no access
		return AuthorizationStatus.accessDenied("User lacks permission to access Activity "+activityId);
	}
	
	@Override
	public boolean isUserCreatorOrAdmin(UserInfo userInfo, String creator) {
		// Admins can see anything.
		if (userInfo.isAdmin()) return true;
		// Only the creator can see the raw file handle
		return userInfo.getId().toString().equals(creator);
	}

	@Override
	public AuthorizationStatus canAccessRawFileHandleByCreator(UserAuthorization userAuthorization, String fileHandleId, String creator) {
		if( isUserCreatorOrAdmin(userAuthorization.getUserInfo(), creator)) {
			return AuthorizationStatus.authorized(); // TODO check scope
		} else {
			return AuthorizationStatus.accessDenied(createFileHandleUnauthorizedMessage(fileHandleId, userAuthorization.getUserInfo().getId()));
		}
	}
	
	/**
	 * Create an unauthorized message for file handles.
	 * 
	 * @param fileHandleId
	 * @param userInfo
	 * @return
	 */
	private String createFileHandleUnauthorizedMessage(String fileHandleId,	Long userId) {
		return String.format(FILE_HANDLE_UNAUTHORIZED_TEMPLATE, fileHandleId, userId.toString());
	}
	


	@Override
	public AuthorizationStatus canAccessRawFileHandleById(UserAuthorization userAuthorization, String fileHandleId) throws NotFoundException {
		// Admins can do anything
		if(userAuthorization.getUserInfo().isAdmin()) return AuthorizationStatus.authorized(); // TODO check scope
		// Lookup the creator by
		String creator  = fileHandleDao.getHandleCreator(fileHandleId);
		// Call the other methods
		return canAccessRawFileHandleByCreator(userAuthorization, fileHandleId, creator);
	}

	@Deprecated
	@Override
	public void canAccessRawFileHandlesByIds(UserAuthorization userAuthorization, List<String> fileHandleIds, Set<String> allowed, Set<String> disallowed)
			throws NotFoundException {
		// no file handles, nothing to do
		if (fileHandleIds.isEmpty()) {
			return;
		}

		// Admins can do anything
		if (userAuthorization.getUserInfo().isAdmin()) { // TODO check scopes
			allowed.addAll(fileHandleIds);
			return;
		}

		// Lookup the creators
		Multimap<String, String> creatorMap = fileHandleDao.getHandleCreators(fileHandleIds);
		for (Entry<String, Collection<String>> entry : creatorMap.asMap().entrySet()) {
			String creator = entry.getKey();
			if (canAccessRawFileHandleByCreator(userAuthorization, "", creator).isAuthorized()) {
				allowed.addAll(entry.getValue());
			} else {
				disallowed.addAll(entry.getValue());
			}
		}
	}

	// TODO we need to think about what it means to authorize based on admin access and ACT membership
	// it might be that the user has to grant these scopes
	@Override
	public boolean isACTTeamMemberOrAdmin(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		if (userInfo.isAdmin()) return true;
		if(userInfo.getGroups() != null) {
			if(userInfo.getGroups().contains(TeamConstants.ACT_TEAM_ID)) return true;
		}
		return false;
	}

	@Override
	public boolean isReportTeamMemberOrAdmin(UserInfo userInfo) throws DatastoreException, UnauthorizedException {
		if (userInfo.isAdmin()) return true;
		if(userInfo.getGroups() != null) {
			if(userInfo.getGroups().contains(TeamConstants.SYNAPSE_REPORT_TEAM_ID)) return true;
		}
		return false;
	}

	/**
	 * Checks whether the parent (or other ancestors) are subject to access restrictions and, if so, whether 
	 * userInfo is a member of the ACT.
	 * 
	 * @param userAuthorization
	 * @param sourceParentId
	 * @param destParentId
	 * @return
	 */
	@Override
	public AuthorizationStatus canUserMoveRestrictedEntity(UserAuthorization userAuthorization, String sourceParentId, String destParentId) throws NotFoundException {
		if (isACTTeamMemberOrAdmin(userAuthorization.getUserInfo())) { // TODO check scope
			return AuthorizationStatus.authorized();
		}
		if (sourceParentId.equals(destParentId)) {
			return AuthorizationStatus.authorized();
		}
		List<Long> sourceParentAncestorIds = nodeDao.getEntityPathIds(sourceParentId);
		List<Long> destParentAncestorIds = nodeDao.getEntityPathIds(destParentId);

		List<String> missingRequirements = accessRequirementDAO.getAccessRequirementDiff(sourceParentAncestorIds, destParentAncestorIds, RestrictableObjectType.ENTITY);
		if (missingRequirements.isEmpty()) { // only OK if destParent has all the requirements that source parent has
			return AuthorizationStatus.authorized();
		} else {
			return AuthorizationStatus.accessDenied("Cannot move restricted entity to a location having fewer access restrictions.");
		}
	}

	@Override
	public AuthorizationStatus canAccessAccessApprovalsForSubject(UserAuthorization userAuthorization,
			RestrictableObjectDescriptor subjectId, ACCESS_TYPE accessType) throws NotFoundException {
		if (isACTTeamMemberOrAdmin(userAuthorization.getUserInfo())) {
			return AuthorizationStatus.authorized(); // TODO check scopes
		} else {
			return AuthorizationStatus.accessDenied("You are not allowed to retrieve access approvals for this subject.");
		}
	}

	@Override
	public boolean isAnonymousUser(UserInfo userInfo) {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		return AuthorizationUtils.isUserAnonymous(userInfo);
	}

	@Override
	public AuthorizationStatus canCreateWiki(UserAuthorization userAuthorization, String objectId, ObjectType objectType) throws DatastoreException, NotFoundException {
		if (objectType==ObjectType.ENTITY) {
			return entityPermissionsManager.canCreateWiki(objectId, userAuthorization);
		} else {
			return canAccess(userAuthorization, objectId, objectType, ACCESS_TYPE.CREATE);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.AuthorizationManager#canDownloadFile(org.sagebionetworks.repo.model.UserInfo, java.util.List)
	 */
	@Override
	public List<FileHandleAuthorizationStatus> canDownloadFile(UserAuthorization userAuthorization,
			List<String> fileHandleIds, String associatedObjectId,
			FileHandleAssociateType associationType) {
		ValidateArgument.required(userAuthorization, "userAuthorization");
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		ObjectType assocatedObjectType = fileHandleAssociationSwitch.getAuthorizationObjectTypeForAssociatedObjectType(associationType);
		// Is the user authorized to download the associated object?
		AuthorizationStatus canUserDownloadAssociatedObject = canAccess(userAuthorization,
				associatedObjectId, assocatedObjectType, ACCESS_TYPE.DOWNLOAD);
		List<FileHandleAuthorizationStatus> results = new ArrayList<FileHandleAuthorizationStatus>(fileHandleIds.size());
		// Validate all all filehandles are actually associated with the given
		// object.
		Set<String> associatedFileHandleIds = fileHandleAssociationSwitch.getFileHandleIdsAssociatedWithObject(fileHandleIds,
						associatedObjectId, associationType);
		// Which file handles did the user create.
		Set<String> fileHandlesCreatedByUser = fileHandleDao.getFileHandleIdsCreatedByUser(userAuthorization.getUserInfo().getId(), fileHandleIds);
		for (String fileHandleId : fileHandleIds) {
			
			if (fileHandlesCreatedByUser.contains(fileHandleId) || userAuthorization.getUserInfo().isAdmin()) { // TODO check scope
				// The user is the creator of the file or and admin so they can
				// download it.
				results.add(new FileHandleAuthorizationStatus(fileHandleId,
						AuthorizationStatus.authorized()));
			} else {
				/*
				 * The user is not an admin and they are not the creator of the
				 * file. Therefore they can only download the file if they have
				 * the download permission on the associated object and the
				 * fileHandle is actually associated with the object.
				 */
				if (associatedFileHandleIds.contains(fileHandleId)) {
					results.add(new FileHandleAuthorizationStatus(fileHandleId,
							canUserDownloadAssociatedObject));
				} else {
					// The fileHandle is not associated with the object.
					results.add(new FileHandleAuthorizationStatus(
							fileHandleId,
							accessDeniedFileNotAssociatedWithObject(fileHandleId, associatedObjectId,
											associationType)));
				}
			}
		}
		return results;
	}

	/**
	 * Create an access denied status for a file handle not associated with the requested object.
	 * @param fileHandleId
	 * @param associatedObjectId
	 * @param associateType
	 * @return
	 */
	public static AuthorizationStatus accessDeniedFileNotAssociatedWithObject(String fileHandleId, String associatedObjectId, FileHandleAssociateType associateType){
		return AuthorizationStatus.accessDenied(String
				.format(FILE_HANDLE_ID_IS_NOT_ASSOCIATED_TEMPLATE,
						fileHandleId, associatedObjectId,
						associateType));
	}


	@Override
	public Set<Long> getAccessibleBenefactors(UserAuthorization userAuthorization, Set<Long> benefactors) { // TODO consider scope?
		Set<Long> results = null;
		if (userAuthorization.getUserInfo().isAdmin()){
			// admin same as input
			results = Sets.newHashSet(benefactors);
		}else{
			// non-adim run a query
			results = this.aclDAO.getAccessibleBenefactors(userAuthorization.getUserInfo().getGroups(), benefactors,
					ObjectType.ENTITY, ACCESS_TYPE.READ);
		}
		// The trash folder should not be in the results
		results.remove(TRASH_FOLDER_ID);
		return results;
	}

	@Override
	public AuthorizationStatus canSubscribe(UserAuthorization userAuthorization, String objectId,
			SubscriptionObjectType objectType)
			throws DatastoreException, NotFoundException {
		if (isAnonymousUser(userAuthorization.getUserInfo())) {
			return AuthorizationStatus.accessDenied(ANONYMOUS_ACCESS_DENIED_REASON);
		}
		switch (objectType) {
			case FORUM:
				Forum forum = forumDao.getForum(Long.parseLong(objectId));
				return canAccess(userAuthorization, forum.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ);
			case THREAD:
				String projectId = threadDao.getProjectId(objectId);
				return canAccess(userAuthorization, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ);
			case DATA_ACCESS_SUBMISSION:
				if (isACTTeamMemberOrAdmin(userAuthorization.getUserInfo())) {
					return AuthorizationStatus.authorized();
				} else {
					return AuthorizationStatus.accessDenied("Only ACT member can follow this topic.");
				}
			case DATA_ACCESS_SUBMISSION_STATUS:
				if (dataAccessSubmissionDao.isAccessor(objectId, userAuthorization.getUserInfo().getId().toString())) {
					return AuthorizationStatus.authorized();
				} else {
					return AuthorizationStatus.accessDenied("Only accessors can follow this topic.");
				}
		}
		return AuthorizationStatus.accessDenied("The objectType is unsubscribable.");
	}

	@Override
	public Set<Long> getAccessibleProjectIds(Set<Long> principalIds) {
		ValidateArgument.required(principalIds, "principalIds");
		if(principalIds.isEmpty()){
			return new HashSet<>(0);
		}
		return this.aclDAO.getAccessibleProjectIds(principalIds, ACCESS_TYPE.READ);
	}
	
	/*
	 * Given a docker repository path, return a valid parent Id, 
	 * a project which has been verified to exist. 
	 * If there is no such valid parent then return null
	 */
	public String validDockerRepositoryParentId(String dockerRepositoryPath) {
		// check that 'repositoryPath' is a valid path
		try {
			DockerNameUtil.validateName(dockerRepositoryPath);
		} catch (IllegalArgumentException e) {
			return null;
		}
		// check that 'repopath' starts with a synapse ID (synID) and synID is a project or folder
		String parentId;
		try {
			parentId = DockerNameUtil.getParentIdFromRepositoryPath(dockerRepositoryPath);
		} catch (IllegalArgumentException e) {
			return null;
		}
		
		if (parentId==null) throw new IllegalArgumentException("parentId is required.");

		try {
			EntityType parentType = nodeDao.getNodeTypeById(parentId);
			if (parentType!=EntityType.project) {
				return null;
			}
		} catch (NotFoundException e) {
			return null;
		}

		return parentId;
	}

	@Override
	public Set<String> getPermittedDockerActions(UserAuthorization userAuthorization, String service, String type, String name, String actionTypes) {
		ValidateArgument.required(userAuthorization, "userAuthorization");
		ValidateArgument.required(service, "service");
		ValidateArgument.required(type, "type");
		ValidateArgument.required(name, "name");
		ValidateArgument.required(actionTypes, "actionTypes");
		
		String[] actionArray = actionTypes.split(",");
		if (REGISTRY_TYPE.equalsIgnoreCase(type)) {
			return getPermittedDockerRegistryActions(userAuthorization, service, name, actionArray);
		} else if (REPOSITORY_TYPE.equalsIgnoreCase(type)) {
			Set<RegistryEventAction> approvedActions = getPermittedDockerRepositoryActions(userAuthorization, service, name, actionArray);
			Set<String> result = new HashSet<String>();
			for (RegistryEventAction a : approvedActions) result.add(a.name());
			return result;
		} else {
			throw new IllegalArgumentException("Unexpected type "+type);
		}
	}

	private Set<String> getPermittedDockerRegistryActions(UserAuthorization userAuthorization, String service, String name, String[] actionTypes) {
		if (name.equalsIgnoreCase(REGISTRY_CATALOG)) {
			// OK, it's a request to list the catalog
			if (userAuthorization.getUserInfo().isAdmin()) {  // TODO consider scope
				// an admin can do *anything*
				return new HashSet<String>(Arrays.asList(actionTypes));
			} else {
				// non-admins cannot list the catalog
				return Collections.emptySet();
			}
		} else {
			// unrecognized name
			return Collections.emptySet();
		}
	}

	private Set<RegistryEventAction> getPermittedDockerRepositoryActions(UserAuthorization userAuthorization, String service, String repositoryPath, String[] actionTypes) {		Set<RegistryEventAction> permittedActions = new HashSet<RegistryEventAction>();

		String repositoryName = service+DockerNameUtil.REPO_NAME_PATH_SEP+repositoryPath;

		String existingDockerRepoId = dockerNodeDao.getEntityIdForRepositoryName(repositoryName);
		
		boolean isInTrash = false;
		if (existingDockerRepoId!=null) {
			String benefactor = nodeDao.getBenefactor(existingDockerRepoId);
			isInTrash = TRASH_FOLDER_ID.equals(KeyFactory.stringToKey(benefactor));
		}
		for (String requestedActionString : actionTypes) {
			RegistryEventAction requestedAction = RegistryEventAction.valueOf(requestedActionString);
			switch (requestedAction) {
			case push:
				// check CREATE or UPDATE permission and add to permittedActions
				AuthorizationStatus as = null;
				if (existingDockerRepoId==null) {
					String parentId = validDockerRepositoryParentId(repositoryPath);
					if (parentId==null) {
						// can't push to a non-existent parent
						as = AuthorizationStatus.accessDenied(""); //TODO: more informative message?
					} else {
						// check for create permission on parent
						as = canCreate(userAuthorization, parentId, EntityType.dockerrepo);
					}
				} else {
					if (!isInTrash) {
						// check update permission on this entity
						as = canAccess(userAuthorization, existingDockerRepoId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
					}
				}
				if (as!=null && as.isAuthorized()) {
					permittedActions.add(requestedAction);
					if (existingDockerRepoId==null) permittedActions.add(pull);
				}
				break;
			case pull:
				if (
					// check DOWNLOAD permission and add to permittedActions
					(existingDockerRepoId!=null && !isInTrash && canAccess(
							userAuthorization, existingDockerRepoId, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD).isAuthorized()) ||
					// If Docker repository was submitted to an Evaluation and if the requester
					// has administrative access to the queue, then DOWNLOAD permission is granted
					evaluationPermissionsManager.isDockerRepoNameInEvaluationWithAccess(repositoryName, 
							userAuthorization.getUserInfo().getGroups(), ACCESS_TYPE.READ_PRIVATE_SUBMISSION)) {
						permittedActions.add(requestedAction);
				}
				break;
			default:
				throw new RuntimeException("Unexpected action type: " + requestedAction);
			}
		}
		return permittedActions;
	}

	@Override
	public void validateHasAccessorRequirement(HasAccessorRequirement req, Set<String> accessors) {
		if (req.getIsCertifiedUserRequired()) {
			if(!groupMembersDao.areMemberOf(
					AuthorizationConstants.BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId().toString(),
					accessors)){
				throw new UserCertificationRequiredException("Accessors must be Synapse Certified Users.");
			}
		}
		if (req.getIsValidatedProfileRequired()) {
			ValidateArgument.requirement(verificationDao.haveValidatedProfiles(accessors),
					"Accessors must have validated profiles.");
		}
	}

	@Override
	public AuthorizationStatus canAccessMembershipInvitation(UserAuthorization userAuthorization, MembershipInvitation mi, ACCESS_TYPE accessType) {
		if (mi.getInviteeId() != null) {
			// The invitee should be able to read or delete the invitation
			boolean userIsInvitee = Long.parseLong(mi.getInviteeId()) == userInfo.getId();
			if (userIsInvitee && (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE)) {
				return AuthorizationStatus.authorized();
			}
		}
		// An admin of the team should be able to create, read or delete the invitation
		boolean userIsTeamAdmin = aclDAO.canAccess(userInfo.getGroups(), mi.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		if (userIsTeamAdmin && (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE || accessType == ACCESS_TYPE.CREATE)) {
			return AuthorizationStatus.authorized();
		}
		// A Synapse admin should have access of any type
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied("Unauthorized to " + accessType +  " membership invitation " + mi.getId());
	}

	@Override
	public AuthorizationStatus canAccessMembershipInvitation(MembershipInvtnSignedToken token, ACCESS_TYPE accessType) {
		String miId = token.getMembershipInvitationId();
		try {
			tokenGenerator.validateToken(token);
		} catch (UnauthorizedException e) {
			return AuthorizationStatus.accessDenied("Unauthorized to access membership invitation " + miId + " (" + e.getMessage() + ")");
		}
		if (accessType == ACCESS_TYPE.READ) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied("Unauthorized to " + accessType +  " membership invitation " + miId);
	}

	@Override
	public AuthorizationStatus canAccessMembershipInvitation(Long userId, InviteeVerificationSignedToken token, ACCESS_TYPE accessType) {
		String miId = token.getMembershipInvitationId();
		try {
			tokenGenerator.validateToken(token);
		} catch (UnauthorizedException e) {
			return AuthorizationStatus.accessDenied("Unauthorized to access membership invitation " + miId + " (" + e.getMessage() + ")");
		}
		if (token.getInviteeId().equals(userId.toString()) && token.getMembershipInvitationId().equals(miId) && accessType == ACCESS_TYPE.UPDATE) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied("Unauthorized to " + accessType +  " membership invitation " + miId);
	}

	@Override
	public AuthorizationStatus canAccessMembershipRequest(UserInfo userInfo, MembershipRequest mr, ACCESS_TYPE accessType) {
		if (userInfo.isAdmin()) {
			return AuthorizationStatus.authorized();
		}
		// An admin of the team should be able to read or delete the request
		// The requester should also be able to read or delete the request
		boolean userIsTeamAdmin = aclDAO.canAccess(userInfo.getGroups(), mr.getTeamId(), ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		boolean userIsRequester = Long.parseLong(mr.getUserId()) == userInfo.getId();
		if ((userIsTeamAdmin || userIsRequester) && (accessType == ACCESS_TYPE.READ || accessType == ACCESS_TYPE.DELETE)) {
			return AuthorizationStatus.authorized();
		}
		return AuthorizationStatus.accessDenied("Unauthorized to " + accessType + " membership request " + mr.getId());
	}
}

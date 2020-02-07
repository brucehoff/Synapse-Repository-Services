package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.manager.util.CollectionUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACL_SCHEME;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.NameIdType;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;

/**
 * The Sage business logic for node management.
 *
 */
public class NodeManagerImpl implements NodeManager {

	static private Log log = LogFactory.getLog(NodeManagerImpl.class);

	private static final Pattern INVALID_ALIAS_CHARACTERS = Pattern.compile("[^a-zA-Z0-9_]");

	public static final Long ROOT_ID = KeyFactory
			.stringToKey(StackConfigurationSingleton.singleton().getRootFolderEntityId());
	public static final Long TRASH_ID = KeyFactory
			.stringToKey(StackConfigurationSingleton.singleton().getTrashFolderEntityId());

	@Autowired
	NodeDAO nodeDao;
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	private AccessControlListDAO aclDAO;
	@Autowired
	private EntityBootstrapper entityBootstrapper;

	@Autowired
	private ActivityManager activityManager;
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.NodeManager#createNewNode(org.
	 * sagebionetworks.repo.model.Node, org.sagebionetworks.repo.model.UserInfo)
	 */
	@WriteTransaction
	@Override
	@Deprecated
	public String createNewNode(Node newNode, UserAuthorization userAuthorization)
			throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException {
		newNode = createNode(newNode, userAuthorization);
		return newNode.getId();
	}

	/**
	 * Create a new node
	 */
	@WriteTransaction
	@Override
	public Node createNode(Node newNode, UserAuthorization userAuthorization)
			throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException {
		// First valid the node
		NodeManagerImpl.validateNode(newNode);
		ValidateArgument.required(userAuthorization, "user authorization information");
		// Also validate the username
		Long userIndividualGroupId = userAuthorization.getUserInfo().getId();
		// Validate the creations data
		NodeManagerImpl.validateNodeCreationData(userIndividualGroupId, newNode);
		// Validate the modified data.
		NodeManagerImpl.validateNodeModifiedData(userIndividualGroupId, newNode);

		// What is the object type of this node
		EntityType type = newNode.getNodeType();

		// By default all nodes inherit their ACL from their parent.
		ACL_SCHEME aclScheme = ACL_SCHEME.INHERIT_FROM_PARENT;

		// If the user did not provide a parent then we use the default
		if (newNode.getParentId() == null) {
			String defaultPath = EntityTypeUtils.getDefaultParentPath(type);
			if (defaultPath == null)
				throw new IllegalArgumentException("There is no default parent for Entities of type: " + type.name()
						+ " so a valid parentId must be provided");
			// Get the parent node.
			String pathId = nodeDao.getNodeIdForPath(defaultPath);
			newNode.setParentId(pathId);
			// Lookup the acl scheme to be used for children of this parent
			aclScheme = entityBootstrapper.getChildAclSchemeForPath(defaultPath);
		} else {
			// does this parent exist?
			if (!nodeDao.isNodeAvailable(newNode.getParentId())) {
				throw new NotFoundException("The given parent: " + newNode.getParentId() + " does not exist");
			}
		}

		// check whether the user is allowed to create this type of node
		authorizationManager.canCreate(userAuthorization, newNode.getParentId(), newNode.getNodeType())
				.checkAuthorizationOrElseThrow();

		// can this entity be added to the parent?
		validateChildCount(newNode.getParentId(), newNode.getNodeType());

		// Handle permission around file handles.
		if (newNode.getFileHandleId() != null) {
			// To set the file handle on a create the caller must have permission
			authorizationManager.canAccessRawFileHandleById(userAuthorization, newNode.getFileHandleId())
					.checkAuthorizationOrElseThrow();

			if (!authorizationManager
					.canAccess(userAuthorization, newNode.getParentId(), ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)
					.isAuthorized()) {
				throw new UnauthorizedException(userAuthorization.getUserInfo().getId().toString()
						+ " is not allowed to upload a file into the chosen folder.");
			}
		}

		// check whether the user is allowed to connect to the specified activity
		canConnectToActivity(newNode.getActivityId(), userAuthorization);

		// If they are allowed then let them create the node
		newNode = nodeDao.createNewNode(newNode);
		String id = newNode.getId();

		// Setup the ACL for this node.
		if (ACL_SCHEME.GRANT_CREATOR_ALL == aclScheme) {
			AccessControlList rootAcl = AccessControlListUtil.createACLToGrantEntityAdminAccess(id,
					userAuthorization.getUserInfo().getId(), new Date());
			aclDAO.create(rootAcl, ObjectType.ENTITY);
		}

		// adding access is done at a higher level, not here
		// authorizationManager.addUserAccess(newNode, userInfo);
		if (log.isDebugEnabled()) {
			log.debug("username: " + userAuthorization.getUserInfo().getId().toString() + " created node: " + id);
		}
		return newNode;
	}

	/**
	 * Validate that number of children for this container has not been exceeded.
	 * 
	 * @param parentId
	 * @param type
	 */
	public void validateChildCount(String parentId, EntityType type) {
		ValidateArgument.required(parentId, "parentId");
		ValidateArgument.required(type, "type");
		// Limits only apply to files, folders, and links
		if (EntityType.file.equals(type) || EntityType.folder.equals(type) || EntityType.link.equals(type)) {
			// There are no limits on the trash or root
			Long parentIdLong = KeyFactory.stringToKey(parentId);
			if (!ROOT_ID.equals(parentIdLong) && !TRASH_ID.equals(parentIdLong)) {
				// Get the child count
				long currentCount = nodeDao.getChildCount(parentId);
				if (currentCount + 1 > StackConfigurationSingleton.singleton()
						.getMaximumNumberOfEntitiesPerContainer()) {
					throw new IllegalArgumentException("Limit of "
							+ StackConfigurationSingleton.singleton().getMaximumNumberOfEntitiesPerContainer()
							+ " children exceeded for parent: " + parentId);
				}
			}
		}
	}

	/**
	 * Validate a node
	 * 
	 * @param userName
	 * @param node
	 */
	public static void validateNode(Node node) {
		if (node == null)
			throw new IllegalArgumentException("Node cannot be null");
		if (node.getNodeType() == null)
			throw new IllegalArgumentException("Node.type cannot be null");
		node.setName(NameValidation.validateName(node.getName()));
		if (StringUtils.isNotEmpty(node.getAlias())) {
			if (INVALID_ALIAS_CHARACTERS.matcher(node.getAlias()).find()) {
				throw new IllegalArgumentException(
						"Aliases can only have letters (a-z and A-Z), digits (0-9) and underscores (_)");
			}
		}
	}

	/**
	 * Make sure the creation data is set, and if not then set it.
	 * 
	 * @param userName
	 * @param newNode
	 * @return
	 */
	public static void validateNodeCreationData(Long userIndividualGroupId, Node newNode) {
		if (userIndividualGroupId == null) {
			throw new IllegalArgumentException("userIndividualGroupId cannot be null");
		}
		if (newNode == null) {
			throw new IllegalArgumentException("New node cannot be null");
		}
		newNode.setCreatedByPrincipalId(userIndividualGroupId);
		newNode.setCreatedOn(new Date(System.currentTimeMillis()));
	}

	/**
	 * When updating a node we clear the 'createdOn' and 'createdBy' fields to tell
	 * NodeDAO not to update them.
	 * 
	 * 
	 * @param existingNode
	 */
	public static void clearNodeCreationDataForUpdate(Node existingNode) {
		if (existingNode == null) {
			throw new IllegalArgumentException("Node cannot be null");
		}
		existingNode.setCreatedByPrincipalId(null);
		existingNode.setCreatedOn(null);
	}

	/**
	 * Make sure the creation data is set, and if not then set it.
	 * 
	 * @param userName
	 * @param newNode
	 * @return
	 */
	static void validateNodeModifiedData(Long userIndividualGroupId, Node newNode) {
		if (userIndividualGroupId == null) {
			throw new IllegalArgumentException("Username cannot be null");
		}
		if (newNode == null) {
			throw new IllegalArgumentException("New node cannot be null");
		}
		newNode.setModifiedByPrincipalId(userIndividualGroupId);
		newNode.setModifiedOn(new Date(System.currentTimeMillis()));
	}

	@WriteTransaction
	@Override
	public void delete(UserAuthorization userAuthorization, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// First validate the username
		ValidateArgument.required(userAuthorization, "user authorization information");
		String userName = userAuthorization.getUserInfo().getId().toString();
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.DELETE)
				.checkAuthorizationOrElseThrow();
		nodeDao.delete(nodeId);

		if (log.isDebugEnabled()) {
			log.debug("username " + userName + " deleted node: " + nodeId);
		}

		aclDAO.delete(nodeId, ObjectType.ENTITY);
	}

	@WriteTransaction
	@Override
	public void deleteVersion(UserAuthorization userAuthorization, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		// First validate the username
		ValidateArgument.required(userAuthorization, "user authorization information");
		authorizationManager.canAccess(userAuthorization, id, ObjectType.ENTITY, ACCESS_TYPE.DELETE)
				.checkAuthorizationOrElseThrow();
		// Lock before we delete
		nodeDao.lockNode(id);
		// Delete while holding the lock.
		nodeDao.deleteVersion(id, versionNumber);
		nodeDao.touch(userAuthorization.getUserInfo().getId(), id);
	}

	@Override
	public Node get(UserAuthorization userAuthorization, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Validate the userAuthorization
		ValidateArgument.required(userAuthorization, "user authorization information");
		String userName = userAuthorization.getUserInfo().getId().toString();
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();

		Node result = nodeDao.getNode(nodeId);
		if (log.isDebugEnabled()) {
			log.debug("username " + userName + " fetched node: " + result.getId());
		}
		return result;
	}

	@Override
	public Node getNodeForVersionNumber(UserAuthorization userAuthorization, String nodeId, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(userAuthorization, "user authorization information");
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		Node result = nodeDao.getNodeForVersion(nodeId, versionNumber);
		return result;
	}

	@WriteTransaction
	@Override
	public Node update(UserAuthorization userAuthorization, Node updatedNode,
			org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations, boolean newVersion)
			throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {

		ValidateArgument.required(userAuthorization, "user authorization information");
		NodeManagerImpl.validateNode(updatedNode);
		// Validation for update only
		ValidateArgument.required(updatedNode.getId(), "The id of the node");
		ValidateArgument.required(updatedNode.getETag(), "The eTag of the node");
		ValidateArgument.required(updatedNode.getParentId(), "The parent id of the node");

		// Validate that the user can update the node.
		authorizationManager.canAccess(userAuthorization, updatedNode.getId(), ObjectType.ENTITY, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();

		Node oldNode = nodeDao.getNode(updatedNode.getId());

		// note, this is duplicated in 'updateNode', below
		final String parentInDatabase = oldNode.getParentId();
		final String parentInUpdate = updatedNode.getParentId();
		authorizationManager.canUserMoveRestrictedEntity(userAuthorization, parentInDatabase, parentInUpdate)
				.checkAuthorizationOrElseThrow();

		if (!StringUtils.equals(oldNode.getAlias(), updatedNode.getAlias())) {
			authorizationManager.canChangeSettings(userAuthorization, oldNode).checkAuthorizationOrElseThrow();
		}

		// Validate that the user can assign the file handle if they have it.
		if (updatedNode.getFileHandleId() != null) {
			// First determine if this is a change
			String currentHandleId = nodeDao.getFileHandleIdForVersion(updatedNode.getId(), null);
			if (!updatedNode.getFileHandleId().equals(currentHandleId)) {
				// This is a change so the user must be the creator of the new file handle
				authorizationManager.canAccessRawFileHandleById(userAuthorization, updatedNode.getFileHandleId())
						.checkAuthorizationOrElseThrow();
				authorizationManager
						.canAccess(userAuthorization, updatedNode.getParentId(), ObjectType.ENTITY, ACCESS_TYPE.UPLOAD)
						.checkAuthorizationOrElseThrow();
			}
		}
		updateNode(userAuthorization, updatedNode, entityPropertyAnnotations, newVersion, ChangeType.UPDATE, oldNode);

		return get(userAuthorization, updatedNode.getId());
	}

	private void updateNode(UserAuthorization userAuthorization, Node updatedNode,
			org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations, boolean newVersion,
			ChangeType changeType, Node oldNode) throws ConflictingUpdateException, NotFoundException,
			DatastoreException, UnauthorizedException, InvalidModelException {

		canConnectToActivity(updatedNode.getActivityId(), userAuthorization);

		// Before making any changes lock the node
		lockAndCheckEtag(updatedNode.getId(), updatedNode.getETag());

		// Clear node creation data to make sure NodeDAO does not change the fields
		NodeManagerImpl.clearNodeCreationDataForUpdate(updatedNode);

		// If this is a new version then we need to create a new version before the
		// update
		if (newVersion) {
			// This will create a new version and set the new version to
			// be the current version. Then the rest of the update will then
			// be applied to this new version.
			nodeDao.createNewVersion(updatedNode);
		}
		// Touch the etag, modifiedOn, and modifiedBy.
		String nextETag = nodeDao.touch(userAuthorization.getUserInfo().getId(), updatedNode.getId());

		// Identify if update is a parentId change by comparing our
		// updatedNode's parentId with the parentId in database
		// and update benefactorID/permissions
		final String parentInDatabase = oldNode.getParentId();
		final String parentInUpdate = updatedNode.getParentId();
		// is this a parentId change?
		if (!KeyFactory.equals(parentInDatabase, parentInUpdate)) {
			authorizationManager.canAccess(userAuthorization, parentInUpdate, ObjectType.ENTITY, ACCESS_TYPE.CREATE)
					.checkAuthorizationOrElseThrow();
			// Validate the limits of the new parent
			validateChildCount(parentInUpdate, updatedNode.getNodeType());

			if (NodeUtils.isProjectOrFolder(updatedNode.getNodeType())) {
				// Notify listeners of the hierarchy change to this container.
				transactionalMessenger.sendMessageAfterCommit(updatedNode.getId(), ObjectType.ENTITY_CONTAINER,
						nextETag, ChangeType.UPDATE);
			}
		}

		// Now make the actual update.
		nodeDao.updateNode(updatedNode);

		// Also update the entity property Annotations if provided
		if (entityPropertyAnnotations != null) {
			entityPropertyAnnotations.setEtag(nextETag);
			nodeDao.updateEntityPropertyAnnotations(updatedNode.getId(), entityPropertyAnnotations);
		}

		if (log.isDebugEnabled()) {
			log.debug("username " + userAuthorization.getUserInfo().getId().toString() + " updated node: "
					+ updatedNode.getId() + ", with a new eTag: " + nextETag);
		}
	}

	/**
	 * Lock the given entity and check the passed etag.
	 * 
	 * @param entityId
	 * @param passedEtag
	 * @throws ConflictingUpdateException if the passed etag does not match the
	 *                                    current etag of the entity.
	 */
	void lockAndCheckEtag(String entityId, String passedEtag) {
		final String currentEtag = nodeDao.lockNode(entityId);
		if (!currentEtag.equals(passedEtag)) {
			throw new ConflictingUpdateException("Object: " + entityId
					+ " was updated since you last fetched it, retrieve it again and re-apply the update");
		}
	}

	@Override
	public Annotations getUserAnnotations(UserAuthorization userAuthorization, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		if (nodeId == null)
			throw new IllegalArgumentException("NodeId cannot be null");
		ValidateArgument.required(userAuthorization, "user authorization information");
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return nodeDao.getUserAnnotations(nodeId);
	}

	@Override
	public Annotations getUserAnnotationsForVersion(UserAuthorization userAuthorization, String nodeId,
			Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(userAuthorization, "user authorization information");
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return nodeDao.getUserAnnotationsForVersion(nodeId, versionNumber);
	}

	@Override
	public org.sagebionetworks.repo.model.Annotations getEntityPropertyAnnotations(UserAuthorization userAuthorization,
			String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if (nodeId == null)
			throw new IllegalArgumentException("NodeId cannot be null");
		ValidateArgument.required(userAuthorization, "user authorization information");
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return nodeDao.getEntityPropertyAnnotations(nodeId);
	}

	@Override
	public org.sagebionetworks.repo.model.Annotations getEntityPropertyForVersion(UserAuthorization userAuthorization,
			String nodeId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(userAuthorization, "user authorization information");
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return nodeDao.getEntityPropertyAnnotationsForVersion(nodeId, versionNumber);
	}

	@WriteTransaction
	@Override
	public Annotations updateUserAnnotations(UserAuthorization userAuthorization, String nodeId, Annotations updated)
			throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		ValidateArgument.required(updated, "annotations");
		ValidateArgument.requiredNotEmpty(nodeId, "nodeId");
		ValidateArgument.requiredNotEmpty(updated.getEtag(), "etag");

		ValidateArgument.required(userAuthorization, "user authorization information");
		// This is no longer called from a create PLFM-325
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();
		// Validate that the annotations
		AnnotationsV2Utils.validateAnnotations(updated);
		// Lock the node and check the etag.
		lockAndCheckEtag(nodeId, updated.getEtag());
		// update etag, modifedOn, and modifiedBy
		nodeDao.touch(userAuthorization.getUserInfo().getId(), nodeId);

		nodeDao.updateUserAnnotations(nodeId, updated);
		return getUserAnnotations(userAuthorization, nodeId);
	}

	@Override
	public EntityType getNodeType(UserAuthorization userAuthorization, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		Node node = get(userAuthorization, nodeId);
		return node.getNodeType();
	}

	@Override
	public EntityType getNodeTypeForDeletion(String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		Node node = nodeDao.getNode(nodeId);
		return node.getNodeType();
	}

	@Override
	public List<EntityHeader> getNodePath(UserAuthorization userAuthorization, String nodeId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(userAuthorization, "user authorization information");
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return NameIdType.toEntityHeader(nodeDao.getEntityPath(nodeId));
	}

	@Override
	public List<EntityHeader> getNodePathAsAdmin(String nodeId) throws NotFoundException, DatastoreException {
		// This version does not require authorization.
		return NameIdType.toEntityHeader(nodeDao.getEntityPath(nodeId));
	}

	@WriteTransaction
	@Override
	public Node createNewNode(Node newNode, org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations,
			UserAuthorization userAuthorization)
			throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException {
		// First create the node
		newNode = createNode(newNode, userAuthorization);
		// The eTag really has no meaning yet because nobody has access to this id until
		// we return.
		entityPropertyAnnotations.setEtag(newNode.getETag());
		entityPropertyAnnotations.setId(newNode.getId());
		// Since we just created this node we do not need to lock.
		nodeDao.updateEntityPropertyAnnotations(newNode.getId(), entityPropertyAnnotations);
		return newNode;
	}

	@Override
	public EntityHeader getNodeHeader(UserAuthorization userAuthorization, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(userAuthorization, "user authorization information");
		authorizationManager.canAccess(userAuthorization, entityId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return nodeDao.getEntityHeader(entityId);
	}

	@Override
	public List<EntityHeader> getNodeHeader(UserAuthorization userAuthorization, List<Reference> references)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(references, "references");
		ValidateArgument.required(userAuthorization, "user authorization information");
		List<EntityHeader> results = nodeDao.getEntityHeader(references);
		// Will remove headers they user cannot see.
		return filterUnauthorizedHeaders(userAuthorization, results);
	}

	@Override
	public List<EntityHeader> getNodeHeaderByMd5(UserAuthorization userAuthorization, String md5)
			throws NotFoundException, DatastoreException {

		if (userAuthorization == null) {
			throw new IllegalArgumentException("User authorization information cannot be null.");
		}
		if (md5 == null) {
			throw new IllegalArgumentException("MD5 cannot be null.");
		}
		List<EntityHeader> entityHeaderList = nodeDao.getEntityHeaderByMd5(md5);
		return filterUnauthorizedHeaders(userAuthorization, entityHeaderList);
	}

	@Override
	public List<EntityHeader> filterUnauthorizedHeaders(UserAuthorization userAuthorization,
			List<EntityHeader> toFilter) {
		ValidateArgument.required(userAuthorization, "user authorization information");
		ValidateArgument.required(toFilter, "toFilter");
		if (toFilter.isEmpty()) {
			// nothing to do.
			return toFilter;
		}
		Set<Long> originalBenefactors = new HashSet<Long>(toFilter.size());
		for (EntityHeader header : toFilter) {
			ValidateArgument.required(header.getBenefactorId(), "entityHeader.benefactorId");
			originalBenefactors.add(header.getBenefactorId());
		}
		// find the intersection.
		Set<Long> benefactorIntersection = authorizationManager.getAccessibleBenefactors(userAuthorization,
				originalBenefactors);
		List<EntityHeader> filtered = new LinkedList<EntityHeader>();
		for (EntityHeader header : toFilter) {
			if (benefactorIntersection.contains(header.getBenefactorId())) {
				filtered.add(header);
			}
		}
		return filtered;
	}

	@Override
	public boolean doesNodeHaveChildren(String nodeId) {
		return nodeDao.doesNodeHaveChildren(nodeId);
	}

	@Override
	public List<VersionInfo> getVersionsOfEntity(UserAuthorization userAuthorization, String entityId, long offset,
			long limit) throws NotFoundException, UnauthorizedException, DatastoreException {
		validateReadAccess(userAuthorization, entityId);
		EntityType type = nodeDao.getNodeTypeById(entityId);
		if (EntityType.table.equals(type) || EntityType.entityview.equals(type)) {
			/*
			 * Snapshots do not exist for the current version of tables/views. Therefore the
			 * current version is excluded from the results by incrementing the offset by
			 * one.
			 */
			offset += 1L;
		}
		return nodeDao.getVersionsOfEntity(entityId, offset, limit);
	}

	public void validateReadAccess(UserAuthorization userAuthorization, String entityId) throws NotFoundException {
		ValidateArgument.required(userAuthorization, "user authorization information");
		authorizationManager.canAccess(userAuthorization, entityId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
	}

	@Override
	public Activity getActivityForNode(UserAuthorization userAuthorization, String nodeId, Long versionNumber)
			throws DatastoreException, NotFoundException {
		String activityId = null;
		if (versionNumber != null)
			activityId = nodeDao.getActivityId(nodeId, versionNumber);
		else
			activityId = nodeDao.getActivityId(nodeId);
		return activityManager.getActivity(userAuthorization, activityId);
	}

	@WriteTransaction
	@Override
	public void setActivityForNode(UserAuthorization userAuthorization, String nodeId, String activityId)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		Node toUpdate = get(userAuthorization, nodeId);
		toUpdate.setActivityId(activityId);
		update(userAuthorization, toUpdate, null, false);
	}

	@WriteTransaction
	@Override
	public void deleteActivityLinkToNode(UserAuthorization userAuthorization, String nodeId)
			throws NotFoundException, UnauthorizedException, DatastoreException {
		Node toUpdate = get(userAuthorization, nodeId);
		toUpdate.setActivityId(NodeDAO.DELETE_ACTIVITY_VALUE);
		update(userAuthorization, toUpdate, null, false);
	}

	/*
	 * Private Methods
	 */
	private void canConnectToActivity(String activityId, UserAuthorization userAuthorization) throws NotFoundException {
		if (activityId != null) {
			if (NodeDAO.DELETE_ACTIVITY_VALUE.equals(activityId))
				return;
			if (!activityManager.doesActivityExist(activityId))
				throw new NotFoundException("Activity id " + activityId + " not found.");
			authorizationManager.canAccessActivity(userAuthorization, activityId).checkAuthorizationOrElseThrow();
		}
		authorizationManager.canAccessActivity(userAuthorization, activityId).checkAuthorizationOrElseThrow();
	}

	@Override
	public String getFileHandleIdForVersion(UserAuthorization userAuthorization, String id, Long versionNumber)
			throws NotFoundException, UnauthorizedException {
		// Validate that the user has download permission.
		authorizationManager.canAccess(userAuthorization, id, ObjectType.ENTITY, ACCESS_TYPE.DOWNLOAD)
				.checkAuthorizationOrElseThrow();
		// Get the value from the dao
		String fileHandleId = nodeDao.getFileHandleIdForVersion(id, versionNumber);
		checkFileHandleId(id, fileHandleId);
		return fileHandleId;
	}

	@Override
	public List<Reference> getCurrentRevisionNumbers(List<String> nodeIds) {
		return nodeDao.getCurrentRevisionNumbers(nodeIds);
	}

	/*
	 * Private Methods
	 */

	/**
	 * Throws a NotFoundException when a file handle is null
	 * 
	 * @param entityId
	 * @param fileHandleId
	 * @throws NotFoundException
	 */
	private void checkFileHandleId(String entityId, String fileHandleId) throws NotFoundException {
		if (fileHandleId == null)
			throw new NotFoundException("Object " + entityId + " does not have a file handle associated with it.");
	}

	@Override
	public String getEntityIdForAlias(String alias) {
		return nodeDao.getNodeIdByAlias(alias);
	}

	@Override
	public Set<String> getFileHandleIdsAssociatedWithFileEntity(List<String> fileHandleIds, String entityId) {
		ValidateArgument.required(fileHandleIds, "fileHandleIds");
		ValidateArgument.required(entityId, "entityId");
		List<Long> fileHandleIdsLong = new ArrayList<Long>();
		CollectionUtils.convertStringToLong(fileHandleIds, fileHandleIdsLong);
		Set<Long> returnedFileHandleIds = nodeDao.getFileHandleIdsAssociatedWithFileEntity(fileHandleIdsLong,
				KeyFactory.stringToKey(entityId));
		Set<String> results = new HashSet<String>();
		CollectionUtils.convertLongToString(returnedFileHandleIds, results);
		return results;
	}

	@Override
	public List<EntityHeader> getChildren(String parentId, List<EntityType> includeTypes, Set<Long> childIdsToExclude,
			SortBy sortBy, Direction sortDirection, long limit, long offset) {
		// EntityManager handles all of the business logic for this call.
		return nodeDao.getChildren(parentId, includeTypes, childIdsToExclude, sortBy, sortDirection, limit, offset);
	}

	@Override
	public ChildStatsResponse getChildrenStats(ChildStatsRequest request) {
		// EntityManager handles all of the business logic for this call.
		return nodeDao.getChildernStats(request);
	}

	@Override
	public boolean isEntityEmpty(String entityId) {
		return !nodeDao.doesNodeHaveChildren(entityId);
	}

	@Override
	public String lookupChild(String parentId, String entityName) {
		// EntityManager handles all of the business logic for this call.
		return nodeDao.lookupChild(parentId, entityName);
	}

	@WriteTransaction
	@Override
	public long createSnapshotAndVersion(UserAuthorization userAuthorization, String nodeId, SnapshotRequest request) {
		ValidateArgument.required(userAuthorization, "user authorization information");
		ValidateArgument.required(nodeId, "id");
		if (request == null) {
			request = new SnapshotRequest();
		}
		// User must have the update permission.
		authorizationManager.canAccess(userAuthorization, nodeId, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();
		// Must be authorized to use a provided activity.
		canConnectToActivity(request.getSnapshotActivityId(), userAuthorization);
		/*
		 * Note: An Etag check is not required here because all of the existing data is
		 * copied to the newly created version without modification. This means
		 * concurrent modifications are not possible with this method.
		 */
		nodeDao.lockNode(nodeId);
		// Snapshot the current version.
		long snapshotVersion = nodeDao.snapshotVersion(userAuthorization.getUserInfo().getId(), nodeId, request);
		// Create a new version that is in-progress
		Node nextVersion = nodeDao.getNode(nodeId);
		nextVersion.setVersionComment(TableConstants.IN_PROGRESS);
		nextVersion.setVersionLabel(TableConstants.IN_PROGRESS);
		nextVersion.setActivityId(null);
		nodeDao.createNewVersion(nextVersion);
		nodeDao.touch(userAuthorization.getUserInfo().getId(), nodeId);
		return snapshotVersion;
	}

	@Override
	public long getCurrentRevisionNumber(String entityId) {
		return nodeDao.getCurrentRevisionNumber(entityId);
	}

	@Override
	public String getNodeName(UserInfo userInfo, String nodeId) {
		// Validate that the user has download permission.
		authorizationManager.canAccess(userInfo, nodeId, ObjectType.ENTITY, ACCESS_TYPE.READ)
				.checkAuthorizationOrElseThrow();
		return nodeDao.getNodeName(nodeId);
	}

}

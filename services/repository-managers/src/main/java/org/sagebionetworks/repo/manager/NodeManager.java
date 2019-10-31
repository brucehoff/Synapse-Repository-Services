package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.web.NotFoundException;

public interface NodeManager {

	/**
	 * Use: {@link #createNode(Node, UserInfo)}
	 */
	@Deprecated
	public String createNewNode(Node newNode, UserAuthorization userAuthorization) throws DatastoreException,
			InvalidModelException, NotFoundException, UnauthorizedException;
	
	/**
	 * Create a new node.
	 * @param newNode
	 * @param userAuthorization
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Node createNode(Node newNode, UserAuthorization userAuthorization) throws DatastoreException,InvalidModelException, NotFoundException, UnauthorizedException;
	
	/**
	 * Create a new node with annotations.
	 * @param newNode
	 * @param newAnnotations
	 * @param userAuthorization
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Node createNewNode(Node newNode, org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations, UserAuthorization userAuthorization) throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException;
	
	/**
	 * Delete a node using its id.
	 * @param userName
	 * @param nodeId
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void delete(UserAuthorization userAuthorization, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get a node using its id.
	 * @param userName
	 * @param nodeId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Node get(UserAuthorization userAuthorization, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the full path of a node.
	 * 
	 * @param userAuthorization
	 * @param nodeId
	 * @return The first EntityHeader in the list will be the root parent for this node, and the last
	 * will be the EntityHeader for the given node.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getNodePath(UserAuthorization userAuthorization, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * This should only be called for internal use.
	 * @param nodeId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getNodePathAsAdmin(String nodeId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get a node for a given version number.
	 * @param userAuthorization
	 * @param nodeId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Node getNodeForVersionNumber(UserAuthorization userAuthorization, String nodeId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Update a node and its annotations in the same call.  This means we only need to acquire the lock once.
	 * @param username
	 * @param updatedAnnoations
	 * @param updatedNode
	 * @param newVersion - Should a new version be created for this update?
	 * @throws UnauthorizedException
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public Node update(UserAuthorization userAuthorization, Node updatedNode, org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations, boolean newVersion) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	/**
	 * Update the user annotations of a node.
	 * @param userAuthorization
	 * @param nodeId
	 * @return
	 * @throws ConflictingUpdateException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws InvalidModelException 
	 */
	public Annotations updateUserAnnotations(UserAuthorization userAuthorization, String nodeId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;

	Annotations getUserAnnotations(UserAuthorization userAuthorization, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;

	Annotations getUserAnnotationsForVersion(UserAuthorization userAuthorization, String nodeId, Long versionNumber) throws NotFoundException,
			DatastoreException, UnauthorizedException;

	org.sagebionetworks.repo.model.Annotations getEntityPropertyAnnotations(UserAuthorization userAuthorization, String nodeId) throws NotFoundException, DatastoreException, UnauthorizedException;

	org.sagebionetworks.repo.model.Annotations getEntityPropertyForVersion(UserAuthorization userAuthorization, String nodeId, Long versionNumber) throws NotFoundException,
			DatastoreException, UnauthorizedException;

	/**
	 * Get the node type of an entity
	 * @param userAuthorization
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public EntityType getNodeType(UserAuthorization userAuthorization, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the node type of an entity for deletion
	 * 
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public EntityType getNodeTypeForDeletion(String entityId) throws NotFoundException, DatastoreException,
			UnauthorizedException;

	/**
	 * Get a full header for an entity.
	 * 
	 * @param userAuthorization
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public EntityHeader getNodeHeader(UserAuthorization userAuthorization, String entityId, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get an entity header for each reference.
	 * 
	 * @param userAuthorization
	 * @param references
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getNodeHeader(UserAuthorization userAuthorization, List<Reference> references) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Gets the header information for entities whose file's MD5 matches the given MD5 checksum.
	 */
	public List<EntityHeader> getNodeHeaderByMd5(UserAuthorization userAuthorization, String md5)
			throws NotFoundException, DatastoreException;

	/**
	 * Delete a specific version of a node.
	 * @param userAuthorization
	 * @param id
	 * @param long1
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws ConflictingUpdateException 
	 */
	public void deleteVersion(UserAuthorization userAuthorization, String id, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException;

	/**
	 * Does this node have children?
	 * 
	 * @param entityId
	 * @return
	 */
	public boolean doesNodeHaveChildren(String entityId);

	public List<VersionInfo> getVersionsOfEntity(UserAuthorization userAuthorization,
			String entityId, long offset, long limit) throws NotFoundException, UnauthorizedException, DatastoreException;

	/**
	 * Gets the activity that generated the Node
	 * @param userAuthorization
	 * @param nodeId
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public Activity getActivityForNode(UserAuthorization userAuthorization, String nodeId, Long versionNumber) throws DatastoreException, NotFoundException;

	/**
	 * Sets the activity that generated the current version of the node
	 * @param userAuthorization
	 * @param nodeId
	 * @param activityId
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public void setActivityForNode(UserAuthorization userAuthorization, String nodeId,
			String activityId) throws NotFoundException, UnauthorizedException,
			DatastoreException;

	/**
	 * Deletes the generatedBy relationship between the entity and its activity
	 * @param userAuthorization
	 * @param nodeId
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 */
	public void deleteActivityLinkToNode(UserAuthorization userAuthorization, String nodeId)
			throws NotFoundException, UnauthorizedException, DatastoreException;


	/**
	 * Get the FileHandleId of the file associated with a given version of the entity. The caller must have permission
	 * to downlaod this file to get the handle.
	 * 
	 * @param userAuthorization
	 * @param id
	 * @param versionNumber if null, use current version
	 * @return
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public String getFileHandleIdForVersion(UserAuthorization userAuthorization, String id, Long versionNumber) throws NotFoundException, UnauthorizedException;

	/**
	 * Get a reference for the current version of the given node ids
	 * @param nodeIds node ids to lookup
	 * @return list of References with the current version filled in
	 */
	public List<Reference> getCurrentRevisionNumbers(List<String> nodeIds);

	/**
	 * Given a list of EntityHeaders, return the sub-set of EntityHeaders that the user is authorized to read.
	 * @param userAuthorization
	 * @param toFilter
	 * @return
	 */
	List<EntityHeader> filterUnauthorizedHeaders(UserAuthorization userAuthorization,
			List<EntityHeader> toFilter);

	/**
	 * Lookup an Entity ID using an alias.
	 * @param alias
	 * @return
	 */
	public String getEntityIdForAlias(String alias);

	/**
	 * Return a set of fileHandleIds that associated with entityId and appear in the provided list.
	 * 
	 * @param fileHandleIds
	 * @param entityId
	 * @return
	 */
	public Set<String> getFileHandleIdsAssociatedWithFileEntity(List<String> fileHandleIds, String entityId);

	/**
	 * Get one page of children for a given parentId
	 * @param parentId The id of the parent.
	 * @param includeTypes The types of children to include in the results.
	 * @param childIdsToExclude Child IDs to be excluded from the results.
	 * @param sortBy Sort by. 
	 * @param sortDirection Sort direction
	 * @param limit
	 * @param offset
	 * @return
	 */
	public List<EntityHeader> getChildren(String parentId,
			List<EntityType> includeTypes, Set<Long> childIdsToExclude, SortBy sortBy, Direction sortDirection, long limit, long offset);
	
	/**
	 * Get the statistics for the given parentId and types.
	 * 
	 * @param request
	 * @return
	 */
	public ChildStatsResponse getChildrenStats(ChildStatsRequest request);
	

	/**
	 * Retrieve the entityId for a given parentId and entityName
	 * 
	 * @param parentId
	 * @param entityName
	 * @return
	 */
	public String lookupChild(String parentId, String entityName);
	
	
	/**
	 * Request to create a new snapshot of a table or view. The provided comment,
	 * label, and activity ID will be applied to the current version thereby
	 * creating a snapshot and locking the current version. After the snapshot is
	 * created a new version will be started with an 'in-progress' label.
	 * 
	 * @param userId
	 * @param nodeId
	 * @param comment  Optional. Version comment.
	 * @param label    Optional. Version label.
	 * @param activity Optional. Associate an activity with the new version.
	 * @return The version number that represents the snapshot/
	 */
	public long createSnapshotAndVersion(UserAuthorization userAuthorization, String nodeId, SnapshotRequest request);

	/**
	 * Get the current revision number for the given Entity Id.
	 * @param entityId
	 * @return
	 */
	long getCurrentRevisionNumber(String entityId);

}

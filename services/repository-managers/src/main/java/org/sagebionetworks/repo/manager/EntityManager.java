package org.sagebionetworks.repo.manager;

import java.util.List;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * A manager for basic editing of entities.
 * 
 * @author jmhill
 *
 */
public interface EntityManager {

	/**
	 * Create a new data.
	 * @param userAuthorization
	 * @param newEntity
	 * @return
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws UnauthorizedException 
	 */
	public <T extends Entity> String createEntity(UserAuthorization userAuthorization, T newEntity, String activityId) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException;
		
	/**
	 * Get an existing dataset
	 * @param userAuthorization
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public <T extends Entity> T getEntity(UserAuthorization userAuthorization, String entityId, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the full path of an entity.
	 * 
	 * @param userAuthorization
	 * @param entityId
	 * @return The first EntityHeader in the list will be the root parent for this node, and the last
	 * will be the EntityHeader for the given node.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getEntityPath(UserAuthorization userAuthorization, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Get the full path of an entity as a '/' separated string
	 * 
	 * @param userAuthorization
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public String getEntityPathAsFilePath(UserAuthorization userAuthorization, String entityId) throws NotFoundException, DatastoreException,
			UnauthorizedException;

	/**
	 * This version of should only be used for validation, and should not be exposed directly to the caller.
	 * 
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public List<EntityHeader> getEntityPathAsAdmin(String entityId) throws NotFoundException, DatastoreException;
	
	/**
	 * Get the type of an entity
	 * @param userAuthorization
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public EntityType getEntityType(UserAuthorization userAuthorization, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the type of an entity for purposes of a delete action
	 * 
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public EntityType getEntityTypeForDeletion(String entityId) throws NotFoundException, DatastoreException,
			UnauthorizedException;

	/**
	 * Get the entity header.
	 * 
	 * @param userAuthorization
	 * @param entityId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public EntityHeader getEntityHeader(UserAuthorization userAuthorization, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
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
	public List<EntityHeader> getEntityHeader(UserAuthorization userAuthorization, List<Reference> references) throws NotFoundException, DatastoreException, UnauthorizedException;


	/**
	 * Delete an existing entity. This is deprecated and should never be exposed from the API (The cascade of container deletion is limited to 15 levels of depth).
	 * 
	 * @param userAuthorization
	 * @param entityId
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
@Deprecated
	public void deleteEntity(UserAuthorization userAuthorization, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Delete a specfic version of an entity
	 * @param userAuthorization
	 * @param id
	 * @param versionNumber
	 * @throws ConflictingUpdateException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void deleteEntityVersion(UserAuthorization userAuthorization, String id, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException;
	
	/**
	 * Get the annotations of an entity.
	 * @param userAuthorization
	 * @param entityId
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Annotations getAnnotations(UserAuthorization userAuthorization, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * Get the annotations of an entity for a given version.
	 * @param userAuthorization
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Annotations getAnnotationsForVersion(UserAuthorization userAuthorization, String id, Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException;
	
	/**
	 * update a datasets annotations 
	 * @param userAuthorization
	 * @param updated
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public void updateAnnotations(UserAuthorization userAuthorization, String entityId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException;
	
	/**
	 * Update a dataset.
	 * @param userAuthorization
	 * @param updated
	 * @param newVersion should a new version be created for this update?
	 * @param activityId Activity id for version. Activity id for entity will not be updated if new version is false and activity id is null
	 * @return True if this update created a new version of the entity.  Note: There are cases where the provided newVersion is false, but a 
	 * new version is automatically created anyway.
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws ConflictingUpdateException 
	 * @throws InvalidModelException 
	 */
	public <T extends Entity> boolean updateEntity(UserAuthorization userAuthorization, T updated, boolean newVersion, String activityId) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException;

	/**
	 * Get a specific version of an entity.
	 * @param <T>
	 * @param userAuthorization
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public <T extends Entity> T getEntityForVersion(UserAuthorization userAuthorization, String entityId, Long versionNumber, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException;

	/**
	 * Gets the entity whose file's MD5 is the same as the specified MD5 string.
	 */
	public List<EntityHeader> getEntityHeaderByMd5(UserAuthorization userAuthorization, String md5)
			throws NotFoundException, DatastoreException;

	/**
	 * Validate that the user has read access.
	 * 
	 * @param userId
	 * @param entityId
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public void validateReadAccess(UserAuthorization userAuthorization, String entityId) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/**
	 * Dev Note: since the user has update permission, we do not need to check
	 * whether they have signed the use agreement, also this is just for uploads
	 * 
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public void validateUpdateAccess(UserAuthorization userAuthorization, String entityId) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/**
	 * Does an entity have children?
	 * @param userAuthorization
	 * @param entityId
	 * @return
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 */
	public boolean doesEntityHaveChildren(UserAuthorization userAuthorization, String entityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Return a paginated list of all version of this entity.
	 * @param userAuthorization
	 * @param entityId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public List<VersionInfo> getVersionsOfEntity(UserAuthorization userAuthorization, String entityId, long offset, long limit) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Gets the activity for the given Entity
	 * @param userAuthorization
	 * @param entityId
	 * @param versionNumber
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public Activity getActivityForEntity(UserAuthorization userAuthorization, String entityId,
			Long versionNumber) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Sets the activity for the current version of the Entity
	 * @param userAuthorization
	 * @param entityId
	 * @param activityId
	 * @return
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public Activity setActivityForEntity(UserAuthorization userAuthorization, String entityId,
			String activityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Deletes the activity generated by relationship to the current version of the Entity
	 * @param userAuthorization
	 * @param entityId
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * @throws NotFoundException
	 */
	public void deleteActivityForEntity(UserAuthorization userAuthorization, String entityId)
			throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Get the FileHandle ID for a given version number.
	 * @param userAuthorization
	 * @param id
	 * @param versionNumber
	 * @return
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 */
	public String getFileHandleIdForVersion(UserAuthorization userAuthorization, String id, Long versionNumber)
			throws UnauthorizedException, NotFoundException;

	/**
	 * Get a reference for the current version of the given entity ids
	 * 
	 * @param entityIds entities ids to lookup
	 * @return list of References with the current version filled in
	 */
	public List<Reference> getCurrentRevisionNumbers(List<String> entityIds);

	/**
	 * Get an entity with just the ID.
	 * @param user
	 * @param entityId
	 * @return
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws DatastoreException 
	 */
	public Entity getEntity(UserAuthorization userAuthorization, String entityId) throws DatastoreException, UnauthorizedException, NotFoundException;

	/**
	 * Lookup an Entity ID using an alias.
	 * @param alias
	 * @return
	 */
	public String getEntityIdForAlias(String alias);
	
	/**
	 * A consistent query to get a page children for a given container.
	 *  
	 * @param user
	 * @param request
	 * @return
	 */
	public EntityChildrenResponse getChildren(UserAuthorization userAuthorization, EntityChildrenRequest request);

	/**
	 * Retrieve the entityId based on its name and parentId.
	 * 
	 * @param userAuthorization
	 * @param request
	 * @return
	 */
	public EntityId lookupChild(UserAuthorization userAuthorization, EntityLookupRequest request);

	/**
	 * Change the given entity's {@link DataType}
	 * @param userAuthorization
	 * @param id
	 * @param dataType
	 * @return
	 */
	public DataTypeResponse changeEntityDataType(UserAuthorization userAuthorization, String id, DataType dataType);
}

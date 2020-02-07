package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Lists;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.MultipartUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
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
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.project.ProjectSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.model.project.StsStorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class EntityManagerImpl implements EntityManager {

	public static final int MAX_NUMBER_OF_REVISIONS = 15000;
	public static final Direction DEFAULT_SORT_DIRECTION = Direction.ASC;
	public static final SortBy DEFAULT_SORT_BY = SortBy.NAME;
	public static final String ROOT_ID = StackConfigurationSingleton.singleton().getRootFolderEntityId();
	public static final List<EntityType> PROJECT_ONLY = Lists.newArrayList(EntityType.project);
	
	public static final int MAX_NAME_CHARS  = 256;
	public static final int MAX_DESCRIPTION_CHARS  = 1000;
	
	@Autowired
	NodeManager nodeManager;
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	@Autowired
	FileHandleManager fileHandleManager;
	@Autowired
	ProjectSettingsManager projectSettingsManager;
	@Autowired
	UserManager userManager;
	@Autowired
	ObjectTypeManager objectTypeManager;
	
	boolean allowCreationOfOldEntities = true;

	//Temporary fields!
	@Autowired
	NodeDAO nodeDAO;

	/**
	 * Injected via spring.
	 * @param allowOldEntityTypes
	 */
	public void setAllowCreationOfOldEntities(boolean allowCreationOfOldEntities) {
		this.allowCreationOfOldEntities = allowCreationOfOldEntities;
	}

	@WriteTransaction
	@Override
	public <T extends Entity> String createEntity(UserInfo userInfo, T newEntity, String activityId)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException {
		validateEntity(newEntity);
		if (newEntity instanceof FileEntity) {
			validateFileEntityStsRestrictions(userInfo, (FileEntity) newEntity);
		}

		// If the parent lives inside an STS-enabled folder, only Files and Folders are allowed.
		if (!(newEntity instanceof FileEntity) && !(newEntity instanceof Folder)) {
			String parentId = newEntity.getParentId();
			if (parentId != null) {
				ProjectSetting projectSetting = projectSettingsManager.getProjectSettingForNode(userInfo, parentId,
						ProjectSettingsType.upload, UploadDestinationListSetting.class);
				if (projectSetting != null && projectSettingsManager.isStsStorageLocationSetting(projectSetting)) {
					throw new IllegalArgumentException("Can only create Files and Folders inside STS-enabled folders");
				}
			}
		}

		// First create a node the represent the entity
		Node node = NodeTranslationUtils.createFromEntity(newEntity);
		// Set the type for this object
		node.setNodeType(EntityTypeUtils.getEntityTypeForClass(newEntity.getClass()));
		node.setActivityId(activityId);
		org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations = new org.sagebionetworks.repo.model.Annotations();
		// Now add all of the annotations and references from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(newEntity, entityPropertyAnnotations);
		// We are ready to create this node
		node = nodeManager.createNewNode(node, entityPropertyAnnotations, userInfo);
		// Return the id of the newly created entity
		return node.getId();
	}

	@Override
	public <T extends Entity> T getEntity(
			UserAuthorization userAuthorization, String entityId, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(entityId, "entityId");
		// Get the annotations for this entity
		org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations = nodeManager.getEntityPropertyAnnotations(userInfo, entityId);
		// Fetch the current node from the server
		Node node = nodeManager.get(userInfo, entityId);
		// Does the node type match the requested type?
		validateType(EntityTypeUtils.getEntityTypeForClass(entityClass),
				node.getNodeType(), entityId);
		return populateEntityWithNodeAndAnnotations(entityClass, entityPropertyAnnotations, node);
	}
	
	@Override
	public Entity getEntity(UserAuthorization userAuthorization, String entityId) throws DatastoreException, UnauthorizedException, NotFoundException {
		// Get the annotations for this entity
		org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations = nodeManager.getEntityPropertyAnnotations(user, entityId);
		// Fetch the current node from the server
		Node node = nodeManager.get(user, entityId);
		return populateEntityWithNodeAndAnnotations(EntityTypeUtils.getClassForType(node.getNodeType()), entityPropertyAnnotations, node);
	}

	/**
	 * Validate that the requested entity type matches the actual entity type.
	 * See http://sagebionetworks.jira.com/browse/PLFM-431.
	 * 
	 * @param <T>
	 * @param requestedType
	 * @param acutalType
	 * @param id
	 */
	private <T extends Entity> void validateType(EntityType requestedType,
			EntityType acutalType, String id) {
		if (acutalType != requestedType) {
			throw new IllegalArgumentException("The Entity: syn" + id
					+ " has an entityType=" + EntityTypeUtils.getEntityTypeClassName(acutalType)
					+ " and cannot be changed to entityType="
					+ EntityTypeUtils.getEntityTypeClassName(requestedType));
		}
	}

	/**
	 * @param <T>
	 * @param userInfo
	 * @param entityId
	 * @param versionNumber
	 * @param entityClass
	 * @return the entity version
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@Override
	public  <T extends Entity> T getEntityForVersion(
			UserAuthorization userAuthorization, String entityId, Long versionNumber,
			Class<? extends T> entityClass) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Get the annotations for this entity
		org.sagebionetworks.repo.model.Annotations annos = nodeManager.getEntityPropertyForVersion(userInfo,
				entityId, versionNumber);
		// Fetch the current node from the server
		Node node = nodeManager.getNodeForVersionNumber(userInfo, entityId,
				versionNumber);
		return populateEntityWithNodeAndAnnotations(entityClass, annos, node);
	}

	/**
	 * Create and populate an instance of an entity using both a node and
	 * annotations.
	 * 
	 * @param <T>
	 * @param entityClass
	 * @param userAnnotations
	 * @param node
	 * @return
	 */
	private <T extends Entity> T populateEntityWithNodeAndAnnotations(
			Class<? extends T> entityClass, org.sagebionetworks.repo.model.Annotations entityProperties, Node node)
			throws DatastoreException, NotFoundException {
		// Return the new object from the dataEntity
		T newEntity = createNewEntity(entityClass);
		// Populate the entity using the annotations and references
		NodeTranslationUtils.updateObjectFromNodeSecondaryFields(newEntity, entityProperties);
		// Populate the entity using the node
		NodeTranslationUtils.updateObjectFromNode(newEntity, node);
		newEntity.setCreatedBy(node.getCreatedByPrincipalId().toString());
		newEntity.setModifiedBy(node.getModifiedByPrincipalId().toString());
		return newEntity;
	}

	@Override
	public List<EntityHeader> getEntityHeaderByMd5(UserInfo userInfo, String md5)
			throws NotFoundException, DatastoreException {
		return nodeManager.getNodeHeaderByMd5(userInfo, md5);
	}

	/**
	 * Will convert the any exceptions to runtime.
	 * 
	 * @param <T>
	 * @param entityClass
	 * @return
	 */
	private <T> T createNewEntity(Class<? extends T> entityClass) {
		T newEntity;
		try {
			newEntity = entityClass.newInstance();
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(
					"Class must have a no-argument constructor: "
							+ Entity.class.getName());
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(
					"Class must have a public no-argument constructor: "
							+ Entity.class.getName());
		}
		return newEntity;
	}

	@WriteTransaction
	@Override
	public void deleteEntity(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity ID cannot be null");
		nodeManager.delete(userInfo, entityId);
	}

	@WriteTransaction
	@Override
	public void deleteEntityVersion(UserInfo userInfo, String id,
			Long versionNumber) throws NotFoundException, DatastoreException,
			UnauthorizedException, ConflictingUpdateException {
		nodeManager.deleteVersion(userInfo, id, versionNumber);
	}

	@Override
	public Annotations getAnnotations(UserAuthorization userAuthorization, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity ID cannot be null");
		// This is a simple pass through
		return nodeManager.getUserAnnotations(userInfo, entityId);
	}

	@Override
	public Annotations getAnnotationsForVersion(UserAuthorization userAuthorization, String id,
												Long versionNumber) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		// Get all of the annotations.
		return nodeManager.getUserAnnotationsForVersion(userInfo, id, versionNumber);
	}

	@WriteTransaction
	@Override
	public void updateAnnotations(UserInfo userInfo, String entityId,
			Annotations updated) throws ConflictingUpdateException,
			NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		if (updated == null)
			throw new IllegalArgumentException("Annoations cannot be null");
		// The user has updated the additional annotations.
		nodeManager.updateUserAnnotations(userInfo, entityId, updated);
	}

	@WriteTransaction
	@Override
	public <T extends Entity> boolean updateEntity(UserInfo userInfo, T updated,
			boolean newVersion, String activityId) throws NotFoundException, DatastoreException,
			UnauthorizedException, ConflictingUpdateException,
			InvalidModelException {

		validateEntity(updated);
		
		if (updated.getId() == null) {
			throw new IllegalArgumentException("The id of the entity should be present");
		}
		
		if (updated.getParentId() == null) {
			throw new IllegalArgumentException("The parentId of the entity should be present");
		}

		if (updated instanceof FileEntity) {
			validateFileEntityStsRestrictions(userInfo, (FileEntity) updated);
		}

		Node node = nodeManager.get(userInfo, updated.getId());
		// Now get the annotations for this node
		org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations = nodeManager.getEntityPropertyAnnotations(userInfo,
				updated.getId());
		
		// Auto-version FileEntity See PLFM-1744
		if(!newVersion && (updated instanceof FileEntity)){
			FileEntity updatedFile = (FileEntity) updated;
			if(!updatedFile.getDataFileHandleId().equals(node.getFileHandleId())){
				newVersion = true;
				// setting this to null we cause the revision id to be used.
				updatedFile.setVersionLabel(null);
				updatedFile.setVersionComment(null);
			}
		}

		if(updated instanceof TableEntity || updated instanceof EntityView) {
			/*
			 * Fix for PLFM-5702. Creating a new version is fundamentally different than
			 * creating a table/view snapshot. We cannot block callers from creating new
			 * versions of tables/views because the Python/R client syn.store() method
			 * automatically sets 'newVersion'=true. Therefore, to prevent users from
			 * breaking their tables/views by explicitly creating new entity versions, we
			 * unconditionally ignore this parameter for table/views.
			 */
			newVersion = false;
		}

		final boolean newVersionFinal = newVersion;
		
		if(newVersion) {
			long currentRevisionNumber = nodeManager.getCurrentRevisionNumber(updated.getId());
			if(currentRevisionNumber + 1 > MAX_NUMBER_OF_REVISIONS) {
				throw new IllegalArgumentException("Exceeded the maximum number of "+MAX_NUMBER_OF_REVISIONS+" versions for a single Entity");
			}
		}
		
		// Set activityId if new version or if not changing versions and activityId is defined
		if(newVersionFinal || (!newVersionFinal && activityId != null)) {
			node.setActivityId(activityId);
		}
		
		updateNodeAndAnnotationsFromEntity(updated, node, entityPropertyAnnotations);
		// Now update both at the same time
		nodeManager.update(userInfo, node, entityPropertyAnnotations, newVersionFinal);
		return newVersionFinal;
	}

	/**
	 * Will update both the passed node and annotations using the passed entity
	 * 
	 * @param <T>
	 * @param entity
	 * @param node
	 * @param annos
	 */
	private <T extends Entity> void updateNodeAndAnnotationsFromEntity(
			T entity, Node node, org.sagebionetworks.repo.model.Annotations annos) {
		// Update the annotations from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(entity, annos);
		// Update the node from the entity
		NodeTranslationUtils.updateNodeFromObject(entity, node);
		// Set the Annotations Etag
		annos.setEtag(entity.getEtag());
	}

	@Override
	public EntityType getEntityType(UserAuthorization userAuthorization, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return nodeManager.getNodeType(userInfo, entityId);
	}

	@Override
	public EntityType getEntityTypeForDeletion(String entityId) throws NotFoundException, DatastoreException {
		return nodeManager.getNodeTypeForDeletion(entityId);
	}

	@Override
	public EntityHeader getEntityHeader(UserAuthorization userAuthorization, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return nodeManager.getNodeHeader(userInfo, entityId);
	}
	
	@Override
	public List<EntityHeader> getEntityHeader(UserAuthorization userAuthorization,
			List<Reference> references) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		return nodeManager.getNodeHeader(userInfo, references);
	}

	@Override
	public List<VersionInfo> getVersionsOfEntity(UserAuthorization userAuthorization, String entityId,
			long offset, long limit) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		// pass through
		return nodeManager.getVersionsOfEntity(userInfo, entityId, offset, limit);
	}

	@Override
	public List<EntityHeader> getEntityPath(UserAuthorization userAuthorization, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// pass through
		return nodeManager.getNodePath(userInfo, entityId);
	}

	@Override
	public String getEntityPathAsFilePath(UserAuthorization userAuthorization, String entityId) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		List<EntityHeader> entityPath = getEntityPath(userInfo, entityId);

		// we skip the root node
		int startIndex = 1;
		if (entityPath.size() == 1) {
			startIndex = 0;
		}
		StringBuilder path = new StringBuilder(256);

		for (int i = startIndex; i < entityPath.size(); i++) {
			if (path.length() > 0) {
				path.append(MultipartUtils.FILE_TOKEN_TEMPLATE_SEPARATOR);
			}
			path.append(entityPath.get(i).getName());
		}
		return path.toString();
	}

	@Override
	public List<EntityHeader> getEntityPathAsAdmin(String entityId)
			throws NotFoundException, DatastoreException {
		// pass through
		return nodeManager.getNodePathAsAdmin(entityId);
	}
	
	@Override
	public void validateReadAccess(UserAuthorization userAuthorization, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		if (!entityPermissionsManager.hasAccess(entityId,
				ACCESS_TYPE.READ, userInfo).isAuthorized()) {
			throw new UnauthorizedException(
					"update access is required to obtain an S3Token for entity "
							+ entityId);
		}
	}
	
	@Override
	public void validateUpdateAccess(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		if (!entityPermissionsManager.hasAccess(entityId,
				ACCESS_TYPE.UPDATE, userInfo).isAuthorized()) {
			throw new UnauthorizedException(
					"update access is required to obtain an S3Token for entity "
							+ entityId);
		}
	}

	@Override
	public boolean doesEntityHaveChildren(UserInfo userInfo, String entityId) throws DatastoreException, UnauthorizedException, NotFoundException {
		validateReadAccess(userInfo, entityId);
		return nodeManager.doesNodeHaveChildren(entityId);
	}

	@Override
	public Activity getActivityForEntity(UserAuthorization userAuthorization, String entityId,
			Long versionNumber) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		return nodeManager.getActivityForNode(userInfo, entityId, versionNumber);		
	}

	@WriteTransaction
	@Override	
	public Activity setActivityForEntity(UserInfo userInfo, String entityId,
			String activityId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		validateUpdateAccess(userInfo, entityId);
		nodeManager.setActivityForNode(userInfo, entityId, activityId);
		return nodeManager.getActivityForNode(userInfo, entityId, null);
	}

	@WriteTransaction
	@Override
	public void deleteActivityForEntity(UserInfo userInfo, String entityId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		validateUpdateAccess(userInfo, entityId);
		nodeManager.deleteActivityLinkToNode(userInfo, entityId);
	}

	@Override
	public String getFileHandleIdForVersion(UserAuthorization userAuthorization, String id, Long versionNumber)
			throws UnauthorizedException, NotFoundException {
		// The manager handles this call.
		return nodeManager.getFileHandleIdForVersion(userInfo, id, versionNumber);
	}

	@Override
	public List<Reference> getCurrentRevisionNumbers(List<String> entityIds) {
		return nodeManager.getCurrentRevisionNumbers(entityIds);
	}

	@Override
	public String getEntityIdForAlias(String alias) {
		return nodeManager.getEntityIdForAlias(alias);
	}

	@Override
	public EntityChildrenResponse getChildren(UserAuthorization userAuthorization,
			EntityChildrenRequest request) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(request, "EntityChildrenRequest");
		if(request.getParentId() == null){
			// Null parentId is used to list projects.
			request.setParentId(ROOT_ID);
			request.setIncludeTypes(PROJECT_ONLY);
		}
		ValidateArgument.required(request.getIncludeTypes(), "EntityChildrenRequest.includeTypes");
		if(request.getIncludeTypes().isEmpty()){
			throw new IllegalArgumentException("EntityChildrenRequest.includeTypes must include at least one type");
		}
		if(request.getSortBy() == null){
			request.setSortBy(DEFAULT_SORT_BY);
		}
		if(request.getSortDirection() == null){
			request.setSortDirection(DEFAULT_SORT_DIRECTION);
		}
		if(!ROOT_ID.equals(request.getParentId())){
			// Validate the caller has read access to the parent
			entityPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, user).checkAuthorizationOrElseThrow();
		}

		// Find the children of this entity that the caller cannot see.
		Set<Long> childIdsToExclude = entityPermissionsManager.getNonvisibleChildren(user, request.getParentId());
		NextPageToken nextPage = new NextPageToken(request.getNextPageToken());
		List<EntityHeader> page = nodeManager.getChildren(
				request.getParentId(), request.getIncludeTypes(),
				childIdsToExclude, request.getSortBy(), request.getSortDirection(), nextPage.getLimitForQuery(),
				nextPage.getOffset());
		// Gather count and size sum if requested.
		ChildStatsResponse stats = nodeManager
				.getChildrenStats(new ChildStatsRequest().withParentId(request.getParentId())
						.withIncludeTypes(request.getIncludeTypes()).withChildIdsToExclude(childIdsToExclude)
						.withIncludeTotalChildCount(request.getIncludeTotalChildCount())
						.withIncludeSumFileSizes(request.getIncludeSumFileSizes()));
		EntityChildrenResponse response = new EntityChildrenResponse();
		response.setPage(page);
		response.setNextPageToken(nextPage.getNextPageTokenForCurrentResults(page));
		response.setTotalChildCount(stats.getTotalChildCount());
		response.setSumFileSizesBytes(stats.getSumFileSizesBytes());
		return response;
	}

	@Override
	public EntityId lookupChild(UserAuthorization userAuthorization, EntityLookupRequest request) {
		ValidateArgument.required(userAuthorization, "userAuthorization");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getEntityName(), "EntityLookupRequest.entityName");
		if(request.getParentId() == null){
			// Null parentId is used to look up projects.
			request.setParentId(ROOT_ID);
		}
		if(!ROOT_ID.equals(request.getParentId())){
			if(!entityPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, userInfo).isAuthorized()){
				throw new UnauthorizedException("Lack of READ permission on the parent entity.");
			}
		}
		String entityId = nodeManager.lookupChild(request.getParentId(), request.getEntityName());
		if(!entityPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo).isAuthorized()){
			throw new UnauthorizedException("Lack of READ permission on the requested entity.");
		}
		EntityId result = new EntityId();
		result.setId(entityId);
		return result;
	}

	@Override
	public DataTypeResponse changeEntityDataType(UserInfo userInfo, String entityId, DataType dataType) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "id");
		ValidateArgument.required(dataType, "DataType");
		return objectTypeManager.changeObjectsDataType(userInfo, entityId, ObjectType.ENTITY, dataType);
	}

	// Validates whether a FileEntity satisfies the STS restrictions of its parent.
	// Package-scoped to facilitate unit tests.
	void validateFileEntityStsRestrictions(UserInfo userInfo, FileEntity fileEntity) {
		// Is the file STS-enabled?
		FileHandle fileHandle = fileHandleManager.getRawFileHandle(userInfo, fileEntity.getDataFileHandleId());
		Long fileStorageLocationId = fileHandle.getStorageLocationId();
		StorageLocationSetting fileStorageLocationSetting = projectSettingsManager.getStorageLocationSetting(
				fileStorageLocationId);
		boolean fileStsEnabled = projectSettingsManager.isStsStorageLocationSetting(fileStorageLocationSetting);

		// Is the parent STS-enabled?
		Long parentStorageLocationId = null;
		boolean parentStsEnabled = false;
		String parentId = fileEntity.getParentId();
		if (parentId != null) {
			UploadDestinationListSetting projectSetting = projectSettingsManager.getProjectSettingForNode(userInfo,
					parentId, ProjectSettingsType.upload, UploadDestinationListSetting.class);
			if (projectSetting != null) {
				// Short-cut: Just grab the first storage location ID. We only compare storage location IDs if STS is
				// enabled, and folders with STS enabled can't have multiple storage locations.
				parentStsEnabled = projectSettingsManager.isStsStorageLocationSetting(projectSetting);
				parentStorageLocationId = projectSetting.getLocations().get(0);
			}
		}

		// If either the file's storage location or the parent's storage location has STS enabled, then the storage
		// locations must be the same. ie, Files in STS-enabled Storage Locations must be placed in a folder with the
		// same storage location, and folders with STS-enabled Storage Locations can only contain files from that
		// storage location.
		if ((fileStsEnabled || parentStsEnabled) && !Objects.equals(fileStorageLocationId, parentStorageLocationId)) {
			// Determine which error message to throw depending on whether the file is STS-enabled or the parent.
			if (fileStsEnabled) {
				throw new IllegalArgumentException("Files in STS-enabled storage locations can only be placed in " +
						"folders with the same storage location");
			}
			//noinspection ConstantConditions
			if (parentStsEnabled) {
				throw new IllegalArgumentException("Folders with STS-enabled storage locations can only accept " +
						"files with the same storage location");
			}
		}
	}

	/**
	 * Validate entity is not null and all values are within limit.
	 * 
	 * @param entity
	 */
	public static void validateEntity(Entity entity) {
		ValidateArgument.required(entity, "entity");
		if(entity.getName() != null) {
			if(entity.getName().length() > MAX_NAME_CHARS) {
				throw new IllegalArgumentException("Name must be "+MAX_NAME_CHARS+" characters or less");				
			}
		}
		if(entity.getDescription() != null) {
			if(entity.getDescription().length() > MAX_DESCRIPTION_CHARS) {
				throw new IllegalArgumentException("Description must be "+MAX_DESCRIPTION_CHARS+" characters or less");
			}
		}
	}
}

package org.sagebionetworks.repo.model;

import java.util.List;

import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;

public interface StorageLocationDAO {

	public Long create(StorageLocationSetting setting);

	public <T extends StorageLocationSetting> T update(T settings) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException;

	public StorageLocationSetting get(Long id) throws DatastoreException, NotFoundException;

	public List<UploadDestinationLocation> getUploadDestinationLocations(List<Long> locations);

	public List<StorageLocationSetting> getAllStorageLocationSettings();
}
package org.sagebionetworks.repo.manager.storagelocation;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;

/**
 * Common interface for processing storage locations, each processor should not depend from the result of other processors
 * 
 * @author Marco
 *
 * @param <T>
 */
public interface StorageLocationProcessor<T extends StorageLocationSetting> {

	/**
	 * Checks if this validator supports the given storage location class
	 * 
	 * @param storageLocationClass
	 * @return
	 */
	boolean supports(Class<? extends StorageLocationSetting> storageLocationClass);
	
	/**
	 * Invoked before translating the given DTO to the DBO object to be saved in the DB
	 * 
	 * @param userInfo The user requesting the creation
	 * @param storageLocation The storage location to validate
	 * @throws IllegalArgumentException If the storage location is not valid
	 */
	void beforeCreate(UserInfo userInfo, T storageLocation);

}

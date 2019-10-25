package org.sagebionetworks.repo.manager;

import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;

public interface ActivityManager {
	
	/**
	 * create Activity
	 * @param <T>
	 * @param userInfo
	 * @param activity
	 * @return the new activity id
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String createActivity(UserInfo userInfo, Activity activity) throws DatastoreException, InvalidModelException;
	

	/**
	 * update an Activity
	 * @param <T>
	 * @param userInfo
	 * @param activity
	 * @return
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public Activity  updateActivity(UserInfo userInfo, Activity activity) throws InvalidModelException, NotFoundException, ConflictingUpdateException, DatastoreException, UnauthorizedException;
	
	/**
	 * delete an Activity
	 * @param userInfo
	 * @param activityId
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public void deleteActivity(UserInfo userInfo, String activityId) throws DatastoreException, UnauthorizedException;

	/**
	 * Get activity for a given activity id
	 * @param userInfo
	 * @param activityId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws UnauthorizedException
	 */
	public Activity getActivity(UserAuthorization userAuthorization, String activityId) throws DatastoreException, NotFoundException, UnauthorizedException;
	
	/**
	 * returns true if activity exists
	 * @param id
	 * @return
	 */
	public boolean doesActivityExist(String id);

	/**
	 * @param userInfo the user making the request
	 * @param activityId activity id
	 * @param limit 0 based limit
	 * @param offset 0 based offset
	 * @return Returns a paginated QueryResults of references that were generated by the given activity id 
	 */
	public PaginatedResults<Reference> getEntitiesGeneratedBy(UserInfo userInfo, String activityId, Integer limit, Integer offset) throws DatastoreException, NotFoundException, UnauthorizedException;

}

package org.sagebionetworks.repo.manager;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class ActivityManagerImpl implements ActivityManager {
	static private Log log = LogFactory.getLog(ActivityManagerImpl.class);

	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private ActivityDAO activityDAO;	
	@Autowired
	AuthorizationManager authorizationManager;	

	/**
	 * For testing
	 * @param idGenerator
	 * @param activityDAO
	 * @param authorizationManager
	 */
	public ActivityManagerImpl(IdGenerator idGenerator,
			ActivityDAO activityDAO, AuthorizationManager authorizationManager) {
		super();
		this.idGenerator = idGenerator;
		this.activityDAO = activityDAO;
		this.authorizationManager = authorizationManager;
	}

	public ActivityManagerImpl() { }
	
	@WriteTransaction
	@Override
	public String createActivity(UserAuthorization userAuthorization, Activity activity)
			throws DatastoreException, InvalidModelException {		

		// for idGenerator based id on create, regardless of what is passed
		activity.setId(idGenerator.generateNewId(IdType.ACTIVITY_ID).toString());

		populateCreationFields(userAuthorization.getUserInfo().getId(), activity);
		return activityDAO.create(activity);
	}
	
	@WriteTransaction
	@Override
	public Activity updateActivity(UserAuthorization userAuthorization, Activity activity)
			throws InvalidModelException, NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException {
		
		// only owner can change
		ValidateArgument.required(userAuthorization, "User Authorization");
		String requestorId = userAuthorization.getUserInfo().getId().toString();
		String requestorName = userAuthorization.getUserInfo().getId().toString();
		Activity currentAct = activityDAO.get(activity.getId());
		if(!userAuthorization.getUserInfo().isAdmin() && !currentAct.getCreatedBy().equals(requestorId)) {
			throw new UnauthorizedException(requestorName +" lacks change access to the requested object.");
		}			
		
		// lock and get new etag
		String neweTag = activityDAO.lockActivityAndGenerateEtag(activity.getId(), activity.getEtag(), ChangeType.UPDATE);
		activity.setEtag(neweTag);
		
		if(log.isDebugEnabled()){
			log.debug("username "+requestorName+" updated activity: "+currentAct.getId());
		}
		populateModifiedFields(userAuthorization.getUserInfo().getId(), activity);
		// update
		return activityDAO.update(activity);
	}

	@WriteTransaction
	@Override
	public void deleteActivity(UserAuthorization userAuthorization, String activityId) throws DatastoreException, UnauthorizedException {				
		Activity activity;
		try {
			activity = activityDAO.get(activityId);
		} catch (NotFoundException ex) {
			return; // don't bug people with 404s on delete
		}
		ValidateArgument.required(userAuthorization, "User Authorization");
		String requestorId = userAuthorization.getUserInfo().getId().toString();
		String requestorName = userAuthorization.getUserInfo().getId().toString();
		// only owner can change
		if(!activity.getCreatedBy().equals(requestorId) && !userAuthorization.getUserInfo().isAdmin()) {
			throw new UnauthorizedException(requestorName +" lacks change access to the requested object.");
		}			
				
		activityDAO.sendDeleteMessage(activity.getId());
		activityDAO.delete(activity.getId());
	}

	@Override
	public Activity getActivity(UserAuthorization userAuthorization, String activityId) 
		throws DatastoreException, NotFoundException, UnauthorizedException {		
		Activity act = activityDAO.get(activityId);
		authorizationManager.canAccessActivity(userAuthorization, activityId).checkAuthorizationOrElseThrow();
		return act;
	}

	@Override
	public boolean doesActivityExist(String id) {
		return activityDAO.doesActivityExist(id);
	}
	
	@Override
	public PaginatedResults<Reference> getEntitiesGeneratedBy(UserAuthorization userAuthorization, String activityId,
			Integer limit, Integer offset) throws DatastoreException, NotFoundException, UnauthorizedException {
		if (offset==null) offset = 0;
		if (limit==null) limit = Integer.MAX_VALUE;
		ServiceConstants.validatePaginationParams((long)offset, (long)limit);

		Activity act = activityDAO.get(activityId);
		authorizationManager.canAccessActivity(userAuthorization, activityId).checkAuthorizationOrElseThrow();
		return activityDAO.getEntitiesGeneratedBy(activityId, limit, offset);
	}

	
	/*
	 * Private Methods
	 */
	static void populateCreationFields(Long userId, Activity a) {
		Date now = new Date();
		a.setCreatedBy(userId.toString());
		a.setCreatedOn(now);
		a.setModifiedBy(userId.toString());
		a.setModifiedOn(now);
	}

	static void populateModifiedFields(Long userId, Activity a) {
		Date now = new Date();
		a.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		a.setCreatedOn(null);
		a.setModifiedBy(userId.toString());
		a.setModifiedOn(now);
	}

}

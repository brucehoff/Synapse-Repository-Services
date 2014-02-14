package org.sagebionetworks.repo.manager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.fileupload.FileItemStream;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.util.jrjc.JRJCHelper;
import org.sagebionetworks.repo.util.jrjc.JiraClient;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AccessRequirementManagerImpl implements AccessRequirementManager {
	
	@Autowired
	private AccessRequirementDAO accessRequirementDAO;		

	@Autowired
	private AuthorizationManager authorizationManager;
	
	@Autowired
	private MessageManager messageManager;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	NodeDAO nodeDao;

	@Autowired
	EvaluationDAO evaluationDAO;

	@Autowired
	private JiraClient jiraClient;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	public AccessRequirementManagerImpl() {}
	
	// for testing 
	public AccessRequirementManagerImpl(
			AccessRequirementDAO accessRequirementDAO,
			AuthorizationManager authorizationManager,
			JiraClient jiraClient,
			MessageManager messageManager,
			FileHandleManager fileHandleManager,
			UserProfileDAO userProfileDAO
	) {
		this.accessRequirementDAO=accessRequirementDAO;
		this.authorizationManager=authorizationManager;
		this.jiraClient=jiraClient;
		this.messageManager=messageManager;
		this.fileHandleManager=fileHandleManager;
		this.userProfileDAO=userProfileDAO;
	}
	
	public static void validateAccessRequirement(AccessRequirement a) throws InvalidModelException {
		if (a.getEntityType()==null ||
				a.getAccessType()==null ||
				a.getSubjectIds()==null) throw new InvalidModelException();
		
		if (!a.getEntityType().equals(a.getClass().getName())) throw new InvalidModelException("entity type differs from class");
	}
	
	public static void populateCreationFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(userInfo.getId().toString());
		a.setCreatedOn(now);
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}

	public static void populateModifiedFields(UserInfo userInfo, AccessRequirement a) {
		Date now = new Date();
		a.setCreatedBy(null); // by setting to null we are telling the DAO to use the current values
		a.setCreatedOn(null);
		a.setModifiedBy(userInfo.getId().toString());
		a.setModifiedOn(now);
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T createAccessRequirement(UserInfo userInfo, T accessRequirement) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		validateAccessRequirement(accessRequirement);
		if (!authorizationManager.canCreateAccessRequirement(userInfo, accessRequirement)) {
			throw new UnauthorizedException();
		}
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirementDAO.create(accessRequirement);
	}
	
	public static ACTAccessRequirement newLockAccessRequirement(UserInfo userInfo, String entityId) {
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);
		// create the 'lock down' access requirement'
		ACTAccessRequirement accessRequirement = new ACTAccessRequirement();
		accessRequirement.setEntityType("org.sagebionetworks.repo.model.ACTAccessRequirement");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setActContactInfo("Access restricted pending review by Synapse Access and Compliance Team.");
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{subjectId}));
		populateCreationFields(userInfo, accessRequirement);
		return accessRequirement;
	}
	
	private S3FileHandle uploadStringToFileHandle(final String userId, final byte[] content) throws IOException, ServiceUnavailableException {
		FileItemStream fis = new FileItemStream() {
			@Override
			public InputStream openStream() throws IOException {return new ByteArrayInputStream(content);}
			@Override
			public String getContentType() {return "text/plain";}
			@Override
			public String getName() {return "body.txt";}
			@Override
			public String getFieldName() {return "none";}
			@Override
			public boolean isFormField() {return false;}
		};
		return fileHandleManager.uploadFile(userId, fis);
	}
	
	private static final String LOCKDOWN_EMAIL_FILE_PATH = "message/lockdownMessage.txt";
	private static final String ENTITY_ID_KEY = "#entityId#";
	
	public MessageToUser newMessageToUser(UserInfo userInfo, String entityId) {
		MessageToUser message = new MessageToUser();
		message.setRecipients(Collections.singleton(userInfo.getId().toString()));
		message.setSubject("Data access restriction");
		// customize message
		String template = ReadResourceUtil.readMailTemplate(LOCKDOWN_EMAIL_FILE_PATH);
		String messageBody = template.replaceAll(ENTITY_ID_KEY, entityId);

		// upload to file handle id
		try {
			S3FileHandle fileHandle = uploadStringToFileHandle(userInfo.getId().toString(), messageBody.getBytes());
			message.setFileHandleId(fileHandle.getId());
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		} catch (ServiceUnavailableException sue) {
			throw new RuntimeException(sue);
		}
		return message;
	}
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public ACTAccessRequirement createLockAccessRequirement(UserInfo userInfo, String entityId) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		// check authority
		if (!(authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.CREATE) &&
				authorizationManager.canAccess(userInfo, entityId, ObjectType. ENTITY, ACCESS_TYPE.UPDATE))) 
			throw new UnauthorizedException();
		
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setId(entityId);
		subjectId.setType(RestrictableObjectType.ENTITY);

		// check whether there is already an access requirement in place
		List<AccessRequirement> ars = accessRequirementDAO.getForSubject(Collections.singletonList(subjectId.getId()), subjectId.getType());
		if (!ars.isEmpty()) throw new IllegalArgumentException("Entity "+entityId+" is already restricted.");
		
		ACTAccessRequirement accessRequirement = newLockAccessRequirement(userInfo, entityId);
		ACTAccessRequirement result  = accessRequirementDAO.create(accessRequirement);
		
		// send email to user
		MessageToUser message = newMessageToUser(userInfo, entityId);
		messageManager.createMessage(userInfo, message);
		
		UserProfile creatorUserProfile = userProfileDAO.get(userInfo.getId().toString());
		String emailString = "";
		List<String> emails = creatorUserProfile.getEmails();
		if (emails!=null && emails.size()>0) emailString = emails.get(0);
		
		// now create the Jira issue
		JRJCHelper.createRestrictIssue(jiraClient, 
				userInfo.getId().toString(), 
				emailString, 
				entityId);

		return result;
	}
	
	@Override
	public QueryResults<AccessRequirement> getAccessRequirementsForSubject(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException {
		List<String> subjectIds = new ArrayList<String>();
		if (RestrictableObjectType.ENTITY==subjectId.getType()) {
			subjectIds.addAll(AccessRequirementUtil.getNodeAncestorIds(nodeDao, subjectId.getId(), true));
		} else {
			subjectIds.add(subjectId.getId());			
		}
		List<AccessRequirement> ars = accessRequirementDAO.getForSubject(subjectIds, subjectId.getType());
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(ars, ars.size());
		return result;
	}
	
	@Override
	public QueryResults<AccessRequirement> getUnmetAccessRequirements(UserInfo userInfo, RestrictableObjectDescriptor subjectId) throws DatastoreException, NotFoundException {
		// first check if there *are* any unmet requirements.  (If not, no further queries will be executed.)
		List<String> subjectIds = new ArrayList<String>();
		subjectIds.add(subjectId.getId());
		List<Long> unmetIds = null;
		if (RestrictableObjectType.ENTITY==subjectId.getType()) {
			List<String> nodeAncestorIds = AccessRequirementUtil.getNodeAncestorIds(nodeDao, subjectId.getId(), false);
			unmetIds = AccessRequirementUtil.unmetAccessRequirementIdsForEntity(
				userInfo, subjectId.getId(), nodeAncestorIds, nodeDao, accessRequirementDAO);
			subjectIds.addAll(nodeAncestorIds);
		} else if (RestrictableObjectType.EVALUATION==subjectId.getType()) {
			unmetIds = AccessRequirementUtil.unmetAccessRequirementIdsForEvaluation(
					userInfo, subjectId.getId(), accessRequirementDAO);
		} else if (RestrictableObjectType.TEAM==subjectId.getType()) {
			unmetIds = AccessRequirementUtil.unmetAccessRequirementIdsForTeam(
					userInfo, subjectId.getId(), accessRequirementDAO);
		} else {
			throw new InvalidModelException("Unexpected object type "+subjectId.getType());
		}
		
		List<AccessRequirement> unmetRequirements = new ArrayList<AccessRequirement>();
		// if there are any unmet requirements, retrieve the object(s)
		if (!unmetIds.isEmpty()) {
			List<AccessRequirement> allRequirementsForSubject = accessRequirementDAO.getForSubject(subjectIds, subjectId.getType());
			for (Long unmetId : unmetIds) { // typically there will be just one id here
				for (AccessRequirement ar : allRequirementsForSubject) { // typically there will be just one id here
					if (ar.getId().equals(unmetId)) unmetRequirements.add(ar);
				}
			}
		}
		QueryResults<AccessRequirement> result = new QueryResults<AccessRequirement>(unmetRequirements, (int)unmetRequirements.size());
		return result;
	}	
	
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends AccessRequirement> T updateAccessRequirement(UserInfo userInfo, String accessRequirementId, T accessRequirement) throws NotFoundException, UnauthorizedException, ConflictingUpdateException, InvalidModelException, DatastoreException {
		validateAccessRequirement(accessRequirement);
		if (!accessRequirementId.equals(accessRequirement.getId().toString()))
			throw new InvalidModelException("Update specified ID "+accessRequirementId+" but object contains id: "+
		accessRequirement.getId());
		verifyCanAccess(userInfo, accessRequirement.getId().toString(), ACCESS_TYPE.UPDATE);
		populateModifiedFields(userInfo, accessRequirement);
		return accessRequirementDAO.update(accessRequirement);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAccessRequirement(UserInfo userInfo,
			String accessRequirementId) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		verifyCanAccess(userInfo, accessRequirementId, ACCESS_TYPE.DELETE);
		accessRequirementDAO.delete(accessRequirementId);
	}
	
	private void verifyCanAccess(UserInfo userInfo, String accessRequirementId, ACCESS_TYPE accessType) throws UnauthorizedException, NotFoundException {
		if (!authorizationManager.canAccess(userInfo, accessRequirementId, ObjectType.ACCESS_REQUIREMENT, accessType)) {
			throw new UnauthorizedException("Unauthorized for "+accessType+" access to AccessRequirement"+accessRequirementId);
		}
	}
}

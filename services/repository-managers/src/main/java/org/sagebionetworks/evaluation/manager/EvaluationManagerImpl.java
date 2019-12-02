package org.sagebionetworks.evaluation.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.evaluation.util.EvaluationUtils;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserAuthorization;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class EvaluationManagerImpl implements EvaluationManager {

	@Autowired
	private EvaluationDAO evaluationDAO;

	@Autowired
	private NodeDAO nodeDAO;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private AuthorizationManager authorizationManager;

	@Autowired
	private EvaluationPermissionsManager evaluationPermissionsManager;
	
	@Autowired
	private EvaluationSubmissionsDAO evaluationSubmissionsDAO;

	@Autowired
	private SubmissionEligibilityManager submissionEligibilityManager;

	@Override
	@WriteTransaction
	public Evaluation createEvaluation(UserAuthorization userAuthorization, Evaluation eval) 
			throws DatastoreException, InvalidModelException, NotFoundException {

		ValidateArgument.required(userAuthorization, "User Authorization");

		final String nodeId = eval.getContentSource();
		if (nodeId == null || nodeId.isEmpty()) {
			throw new IllegalArgumentException("Evaluation " + eval.getId() +
					" is missing content source (are you sure there is Synapse entity for it?).");
		}
		if (!authorizationManager.canAccess(userAuthorization, nodeId, ObjectType. ENTITY, ACCESS_TYPE.CREATE).isAuthorized()) {
			throw new UnauthorizedException("User " + userAuthorization.getUserInfo().getId().toString() +
					" must have " + ACCESS_TYPE.CREATE.name() + " right on the entity " +
					nodeId + " in order to create a evaluation based on it.");
		}

		if (!nodeDAO.getNodeTypeById(nodeId).equals(EntityType.project)) {
			throw new IllegalArgumentException("Evaluation " + eval.getId() +
					" could not be created because the parent entity " + nodeId +  " is not a project.");
		}

		// Create the evaluation
		eval.setName(NameValidation.validateName(eval.getName()));
		eval.setId(idGenerator.generateNewId(IdType.EVALUATION_ID).toString());
		eval.setCreatedOn(new Date());
		String principalId = userAuthorization.getUserInfo().getId().toString();
		String id = evaluationDAO.create(eval, Long.parseLong(principalId));

		// Create the default ACL
		AccessControlList acl =  AccessControlListUtil.
				createACLToGrantEvaluationAdminAccess(eval.getId(), userAuthorization.getUserInfo().getId(), new Date());
		evaluationPermissionsManager.createAcl(userAuthorization, acl);
		
		evaluationSubmissionsDAO.createForEvaluation(KeyFactory.stringToKey(id));

		return evaluationDAO.get(id);
	}

	@Override
	public Evaluation getEvaluation(UserAuthorization userAuthorization, String id)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		EvaluationUtils.ensureNotNull(id, "Evaluation ID");
		evaluationPermissionsManager.hasAccess(userAuthorization, id, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		return evaluationDAO.get(id);
	}
	
	@Override
	public List<Evaluation> getEvaluationByContentSource(UserAuthorization userAuthorization, String id, boolean activeOnly, long limit, long offset)
			throws DatastoreException, NotFoundException {
		EvaluationUtils.ensureNotNull(id, "Entity ID");
		ValidateArgument.required(userAuthorization, "User Authorization");
		evaluationPermissionsManager.canReadEvaluations(userAuthorization).checkAuthorizationOrElseThrow();
		Long now = activeOnly ? System.currentTimeMillis() : null;
		return evaluationDAO.getAccessibleEvaluationsForProject(id, new ArrayList<Long>(userAuthorization.getUserInfo().getGroups()), 
				ACCESS_TYPE.READ, now, limit, offset);
	}

	@Override
	public List<Evaluation> getInRange(UserAuthorization userAuthorization, boolean activeOnly, long limit, long offset)
			throws DatastoreException, NotFoundException {
		Long now = activeOnly ? System.currentTimeMillis() : null;
		return evaluationDAO.getAccessibleEvaluations(new ArrayList<Long>(userAuthorization.getUserInfo().getGroups()), 
				ACCESS_TYPE.READ, now, limit, offset, null);
	}


	@Override
	public List<Evaluation> getAvailableInRange(UserAuthorization userAuthorization, boolean activeOnly, long limit, long offset, List<Long> evaluationIds)
			throws DatastoreException, NotFoundException {
		evaluationPermissionsManager.canReadEvaluations(userAuthorization);
		Long now = activeOnly ? System.currentTimeMillis() : null;
		return evaluationDAO.getAccessibleEvaluations(new ArrayList<Long>(userAuthorization.getUserInfo().getGroups()), 
				ACCESS_TYPE.SUBMIT, now, limit, offset, evaluationIds);
	}

	@Override
	public Evaluation findEvaluation(UserAuthorization userAuthorization, String name)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		EvaluationUtils.ensureNotNull(name, "Name");
		String evalId = evaluationDAO.lookupByName(name);
		Evaluation eval = evaluationDAO.get(evalId);
		if (!evaluationPermissionsManager.hasAccess(userAuthorization, evalId, ACCESS_TYPE.READ).isAuthorized()) {
			eval = null;
		}
		if (eval == null) {
			throw new NotFoundException("No Evaluation found with name " + name);
		}
		return eval;
	}
	
	@Override
	@WriteTransaction
	public Evaluation updateEvaluation(UserAuthorization userAuthorization, Evaluation eval)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		// validate arguments
		EvaluationUtils.ensureNotNull(eval, "Evaluation");
		ValidateArgument.required(userAuthorization, "User Authorization");
		final String evalId = eval.getId();
		
		// validate permissions
		if (!evaluationPermissionsManager.hasAccess(userAuthorization, evalId, ACCESS_TYPE.UPDATE).isAuthorized()) {
			throw new UnauthorizedException("User " + userAuthorization.getUserInfo().getId().toString() +
					" is not authorized to update evaluation " + evalId +
					" (" + eval.getName() + ")");
		}

		// fetch the existing Evaluation and validate changes		
		Evaluation old = evaluationDAO.get(evalId);
		if (old == null) {
			throw new NotFoundException("No Evaluation found with id " + eval.getId());
		}
		validateEvaluation(old, eval);
		
		// perform the update
		evaluationDAO.update(eval);
		return evaluationDAO.get(evalId);
	}
	
	@Override
	public void updateEvaluationEtag(String evalId) throws NotFoundException {
		Evaluation comp = evaluationDAO.get(evalId);
		if (comp == null) throw new NotFoundException("No Evaluation found with id " + evalId);
		evaluationDAO.update(comp);
	}

	@Override
	@WriteTransaction
	public void deleteEvaluation(UserAuthorization userAuthorization, String id) throws DatastoreException, NotFoundException, UnauthorizedException {
		EvaluationUtils.ensureNotNull(id, "Evaluation ID");
		ValidateArgument.required(userAuthorization, "User Authorization");
		Evaluation eval = evaluationDAO.get(id);
		if (eval == null) throw new NotFoundException("No Evaluation found with id " + id);
		evaluationPermissionsManager.hasAccess(userAuthorization, id, ACCESS_TYPE.DELETE).checkAuthorizationOrElseThrow();
		evaluationPermissionsManager.deleteAcl(userAuthorization, id);
		// lock out multi-submission access (e.g. batch updates)
		evaluationSubmissionsDAO.deleteForEvaluation(Long.parseLong(id));
		evaluationDAO.delete(id);
	}

	private static void validateEvaluation(Evaluation oldEval, Evaluation newEval) {
		if (!oldEval.getOwnerId().equals(newEval.getOwnerId())) {
			throw new InvalidModelException("Cannot overwrite Evaluation Owner ID");
		}
		if (!oldEval.getCreatedOn().equals(newEval.getCreatedOn())) {
			throw new InvalidModelException("Cannot overwrite CreatedOn date");
		}
	}

	@Override
	public TeamSubmissionEligibility getTeamSubmissionEligibility(UserAuthorization userAuthorization, String evalId, String teamId) throws NumberFormatException, DatastoreException, NotFoundException
	{
		evaluationPermissionsManager.canCheckTeamSubmissionEligibility(userAuthorization,  evalId,  teamId).checkAuthorizationOrElseThrow();
		return submissionEligibilityManager.getTeamSubmissionEligibility(evaluationDAO.get(evalId), teamId);
	}

}

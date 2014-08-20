package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.repo.model.quiz.Quiz;
import org.sagebionetworks.repo.model.quiz.QuizResponse;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * 
 * @author brucehoff
 *
 */
public interface CertifiedUserManager {
	
	/**
	 * Get the Certified Users Quiz
	 * @return
	 */
	public Quiz getCertificationQuiz();
	
	/**
	 * Store the response and score it. If the user passes they are added to the 
	 * Certified Users group
	 * 
	 * @parm userInfo
	 * @param response
	 * @return
	 * @throws NotFoundException 
	 */
	public PassingRecord submitCertificationQuizResponse(UserInfo userInfo, QuizResponse response) throws NotFoundException;

	/**
	 * Retrieve the questionnaire responses in the system, optionally filtering by user Id.
	 * Must be a Synapse admin to make this call
	 * @param userInfo
	 * @param questionnaireId
	 * @param principalId
	 * @param limit
	 * @param offset
	 * @return
	 */
	public PaginatedResults<QuizResponse> getQuizResponses(UserInfo userInfo, Long principalId, long limit, long offset);
	
	/**
	 * Delete a Quiz Response
	 * @param userInfo
	 * @param responseId
	 * @throws NotFoundException 
	 */
	public void deleteQuizResponse(UserInfo userInfo, Long responseId) throws NotFoundException;
	
	
	/**
	 * Get the info about the user (indicated by principalId) passing the test.
	 * Requestor must be the 
	 * @param userInfo
	 * @param principalId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public PassingRecord getPassingRecord(UserInfo userInfo, Long principalId) throws DatastoreException, NotFoundException;

    /**
     * Get all Passing Records for a given user.
     * Must be a Synapse admin to make this call
     * @param userId
     * @param questionnaireId
     * @param principalId
     * @param limit
     * @param offset
     * @throws NotFoundException
     * @throws DatastoreException 
     */
	public PaginatedResults<PassingRecord> getPassingRecords(UserInfo userInfo, Long principalId, long limit, long offset) throws DatastoreException, NotFoundException;
}

package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.sagebionetworks.repo.model.dbo.dao.MembershipRequestUtils.deserialize;
import static org.sagebionetworks.util.ZipUtils.unzip;
import static org.sagebionetworks.util.ZipUtils.zip;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MembershipRequest;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOMembershipRequestTest {
	
	@Autowired
	private DBOBasicDao dboBasicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private List<Long> toDelete = null;
	private List<Long> teamToDelete = null;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOMembershipRequest.class, params);
			}
		}
		if(dboBasicDao != null && teamToDelete != null){
			for(Long id: teamToDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectByPrimaryKey(DBOTeam.class, params);
			}
		}
	}
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
		teamToDelete = new LinkedList<Long>();
	}
	
	public static DBOMembershipRequest newMembershipRequest(
			IdGenerator idGenerator, 
			DBOBasicDao dboBasicDao) {
		DBOMembershipRequest request = new DBOMembershipRequest();
		request.setId(idGenerator.generateNewId(IdType.MEMBERSHIP_REQUEST_SUBMISSION_ID));
		request.setCreatedOn(System.currentTimeMillis());
		request.setExpiresOn(System.currentTimeMillis());
		DBOTeam team = DBOTeamTest.newTeam();
		team = dboBasicDao.createNew(team);
		request.setTeamId(team.getId());
		Long userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		request.setUserId(userId);
		request.setProperties((new String("abcdefg")).getBytes());
		return request;
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, NotFoundException, UnsupportedEncodingException{
		DBOMembershipRequest request = newMembershipRequest(idGenerator, dboBasicDao);
		// Make sure we can create it
		DBOMembershipRequest clone = dboBasicDao.createNew(request);
		toDelete.add(request.getId());
		teamToDelete.add(request.getTeamId());
		assertNotNull(clone);
		assertEquals(request, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", request.getId());
		clone = dboBasicDao.getObjectByPrimaryKey(DBOMembershipRequest.class, params);
		assertNotNull(clone);
		assertEquals(request, clone);
		
		// Make sure we can update it.
		clone.setProperties(new byte[] { (byte)1 });
		boolean result = dboBasicDao.update(clone);
		assertTrue(result);
		
		// Get the clone back again
		params = new MapSqlParameterSource();
		params.addValue("id", clone.getId());
		DBOMembershipRequest clone2 = dboBasicDao.getObjectByPrimaryKey(DBOMembershipRequest.class, params);
		assertEquals(clone, clone2);
	}

	@Test
	public void testTranslatorRefactor() throws IOException {
		DBOMembershipRequest backup = new DBOMembershipRequest();
		String backupProperties =
				"<MembershipRqstSubmission>\n" +
				"  <createdOn>2014-02-05 22:21:37.765 UTC</createdOn>\n" +
				"  <createdBy>1976831</createdBy>\n" +
				"  <userId>2223382</userId>\n" +
				"  <teamId>2223746</teamId>\n" +
				"</MembershipRqstSubmission>\n";
		backup.setProperties(zip(backupProperties.getBytes()));

		// Method under test
		DBOMembershipRequest translated = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		String translatedProperties = new String(unzip(translated.getProperties())).trim();
		// Assert that the top level xml tags are updated
		assertTrue(translatedProperties.startsWith("<MembershipRequest>"));
		assertTrue(translatedProperties.endsWith("</MembershipRequest>"));

		String expectedProperties =
				"<MembershipRequest>\n" +
				"  <createdOn>2014-02-05 22:21:37.765 UTC</createdOn>\n" +
				"  <createdBy>1976831</createdBy>\n" +
				"  <userId>2223382</userId>\n" +
				"  <teamId>2223746</teamId>\n" +
				"</MembershipRequest>\n";
		MembershipRequest expectedDTO = deserialize(zip(expectedProperties.getBytes()));
		MembershipRequest translatedDTO = deserialize(translated.getProperties());
		// Assert that the translated DTO's contents are identical to the backup DTO's
		assertEquals(expectedDTO, translatedDTO);
	}

	@Test
	public void testTranslatorRefactorAlreadyTranslated() throws IOException {
		DBOMembershipRequest backup = new DBOMembershipRequest();
		String backupProperties =
				"<MembershipRequest>\n" +
				"  <createdOn>2014-02-05 22:21:37.765 UTC</createdOn>\n" +
				"  <createdBy>1976831</createdBy>\n" +
				"  <userId>2223382</userId>\n" +
				"  <teamId>2223746</teamId>\n" +
				"</MembershipRequest>\n";
		backup.setProperties(zip(backupProperties.getBytes()));

		// Method under test
		DBOMembershipRequest translated = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		String translatedProperties = new String(unzip(translated.getProperties())).trim();
		// Assert that the top level xml tag are still correct
		assertTrue(translatedProperties.startsWith("<MembershipRequest>"));
		assertTrue(translatedProperties.endsWith("</MembershipRequest>"));

		MembershipRequest backupDTO = deserialize(zip(backupProperties.getBytes()));
		MembershipRequest translatedDTO = deserialize(translated.getProperties());
		// Assert that the backup and the translated DTOs are identical
		assertEquals(backupDTO, translatedDTO);
	}
}

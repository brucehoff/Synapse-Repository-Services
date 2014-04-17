package org.sagebionetworks.repo.model.dbo.asynch;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_CHANGED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_COMPRESSED_BODY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ERROR_DETAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ERROR_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_PROGRESS_CURRENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_PROGRESS_MESSAGE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_PROGRESS_TOTAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_STARTED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_STARTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_ASYNCH_JOB_TYPE;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * Database object for a asynchronous table job.
 * 
 * @author John
 *
 */
@Table(name = ASYNCH_JOB_STATUS)
public class DBOAsynchJobStatus implements MigratableDatabaseObject<DBOAsynchJobStatus, DBOAsynchJobStatus> {
	
	/**
	 * The maximum number of characters in a string message.
	 */
	public static final int MAX_MESSAGE_CHARS = 3000;
	
	/**
	 * State of a job
	 */
	public enum JobState{
		PROCESSING,
		FAILED,
		COMPLETE,
	}

	private static TableMapping<DBOAsynchJobStatus> tableMapping = AutoTableMapping.create(DBOAsynchJobStatus.class);
	
	@Field(name = COL_ASYNCH_JOB_ID, nullable = false, primary=true, backupId = true)
	private Long jobId;
	
	@Field(name = COL_ASYNCH_JOB_ETAG, nullable = false, etag = true, varchar=256)
	private String etag;
	
	@Field(name = COL_ASYNCH_JOB_STATE, nullable = false)
	private JobState jobState;
	
	@Field(name = COL_ASYNCH_JOB_TYPE, nullable = false, varchar=1000)
	private AsynchJobType jobType;
	
	@Field(name = COL_ASYNCH_JOB_ERROR_MESSAGE, varchar=MAX_MESSAGE_CHARS, nullable = true)
	private String errorMessage;
	
	@Field(name = COL_ASYNCH_JOB_ERROR_DETAILS, blob="mediumblob", nullable = true)
	private byte[] errorDetails;
	
	@Field(name = COL_ASYNCH_JOB_PROGRESS_CURRENT, nullable = true)
	private Long progressCurrent;

	@Field(name = COL_ASYNCH_JOB_PROGRESS_TOTAL, nullable = true)
	private Long progressTotal;
	
	@Field(name = COL_ASYNCH_JOB_PROGRESS_MESSAGE, nullable = true, varchar=MAX_MESSAGE_CHARS)
	private String progressMessage;
	
	@Field(name = COL_ASYNCH_JOB_STARTED_ON, nullable = false)
	private Long startedOn;
	
	@Field(name = COL_ASYNCH_JOB_STARTED_BY, nullable = false)
	@ForeignKey(table = SqlConstants.TABLE_USER_GROUP, field = SqlConstants.COL_USER_GROUP_ID, cascadeDelete = true)
	private Long startedByUserId;
	
	@Field(name = COL_ASYNCH_JOB_CHANGED_ON, nullable = false)
	private Long changedOn;
	
	@Field(name = COL_ASYNCH_JOB_COMPRESSED_BODY, blob="mediumblob", nullable = false)
	private byte[] compressedBody;

	
	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public JobState getJobState() {
		return jobState;
	}

	public void setJobState(JobState jobState) {
		this.jobState = jobState;
	}

	public AsynchJobType getJobType() {
		return jobType;
	}

	public void setJobType(AsynchJobType jobType) {
		this.jobType = jobType;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public byte[] getErrorDetails() {
		return errorDetails;
	}

	public void setErrorDetails(byte[] errorDetails) {
		this.errorDetails = errorDetails;
	}

	public Long getProgressCurrent() {
		return progressCurrent;
	}

	public void setProgressCurrent(Long progressCurrent) {
		this.progressCurrent = progressCurrent;
	}

	public Long getProgressTotal() {
		return progressTotal;
	}

	public void setProgressTotal(Long progressTotal) {
		this.progressTotal = progressTotal;
	}

	public String getProgressMessage() {
		return progressMessage;
	}

	public void setProgressMessage(String progressMessage) {
		this.progressMessage = progressMessage;
	}

	public Long getStartedOn() {
		return startedOn;
	}

	public void setStartedOn(Long startedOn) {
		this.startedOn = startedOn;
	}

	public Long getChangedOn() {
		return changedOn;
	}

	public void setChangedOn(Long changedOn) {
		this.changedOn = changedOn;
	}

	public Long getStartedByUserId() {
		return startedByUserId;
	}

	public void setStartedByUserId(Long startedByUserId) {
		this.startedByUserId = startedByUserId;
	}

	public byte[] getCompressedBody() {
		return compressedBody;
	}

	public void setCompressedBody(byte[] compressedBody) {
		this.compressedBody = compressedBody;
	}

	public static void setTableMapping(
			TableMapping<DBOAsynchJobStatus> tableMapping) {
		DBOAsynchJobStatus.tableMapping = tableMapping;
	}
	@Override
	public TableMapping<DBOAsynchJobStatus> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.ASYNCH_JOB_STATUS;
	}

	@Override
	public MigratableTableTranslation<DBOAsynchJobStatus, DBOAsynchJobStatus> getTranslator() {
		return new MigratableTableTranslation<DBOAsynchJobStatus, DBOAsynchJobStatus>(){

			@Override
			public DBOAsynchJobStatus createDatabaseObjectFromBackup(
					DBOAsynchJobStatus backup) {
				return backup;
			}

			@Override
			public DBOAsynchJobStatus createBackupFromDatabaseObject(
					DBOAsynchJobStatus dbo) {
				return dbo;
			}
			
		};
	}

	@Override
	public Class<? extends DBOAsynchJobStatus> getBackupClass() {
		return DBOAsynchJobStatus.class;
	}

	@Override
	public Class<? extends DBOAsynchJobStatus> getDatabaseObjectClass() {
		return DBOAsynchJobStatus.class;
	}

	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

}

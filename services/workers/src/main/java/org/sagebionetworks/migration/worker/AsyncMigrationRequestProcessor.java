package org.sagebionetworks.migration.worker;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;

public interface AsyncMigrationRequestProcessor {
	
	public void processAsyncMigrationTypeCountRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationTypeCountRequest mtcReq, final String jobId) throws Throwable;

	public void processAsyncMigrationRangeChecksumRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationRangeChecksumRequest mrcReq, final String jobId) throws Throwable;
	
	public void processAsyncMigrationTypeChecksumRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationTypeChecksumRequest mrcReq, final String jobId) throws Throwable;

	public void processAsyncMigrationRowMetadataRequest(
			final ProgressCallback<Void> progressCallback, final UserInfo user,
			final AsyncMigrationRowMetadataRequest mrcReq, final String jobId) throws Throwable;


}

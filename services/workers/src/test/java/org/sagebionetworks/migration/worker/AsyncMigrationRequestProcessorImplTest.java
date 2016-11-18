package org.sagebionetworks.migration.worker;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.any;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.migration.MigrationManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRangeChecksumResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationRowMetadataResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeChecksumResult;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountRequest;
import org.sagebionetworks.repo.model.migration.AsyncMigrationTypeCountResult;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.springframework.test.util.ReflectionTestUtils;

public class AsyncMigrationRequestProcessorImplTest {

	@Mock
	private AsynchJobStatusManager mockAsynchJobStatusManager;
	@Mock
	private MigrationManager mockMigrationManager;
	@Mock
	ProgressCallback<Void> mockProgressCallback;
	
	ExecutorService migrationExecutorService;
	AsyncMigrationRequestProcessor reqProcessor;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		migrationExecutorService = Executors.newFixedThreadPool(2);
		reqProcessor = new AsyncMigrationRequestProcessorImpl();
		ReflectionTestUtils.setField(reqProcessor, "asynchJobStatusManager", mockAsynchJobStatusManager);
		ReflectionTestUtils.setField(reqProcessor, "migrationManager", mockMigrationManager);
		ReflectionTestUtils.setField(reqProcessor, "migrationExecutorService", migrationExecutorService);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testProcessAsyncMigrationTypeCountRequestOK() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AsyncMigrationTypeCountRequest mtcr = new AsyncMigrationTypeCountRequest();
		mtcr.setType(MigrationType.ACCESS_APPROVAL.name());
		MigrationTypeCount expectedTypeCount = new MigrationTypeCount();
		expectedTypeCount.setType(MigrationType.ACCESS_APPROVAL);
		expectedTypeCount.setCount(100L);
		expectedTypeCount.setMinid(1L);
		expectedTypeCount.setMaxid(199L);
		when(mockMigrationManager.getMigrationTypeCount(eq(userInfo), eq(MigrationType.valueOf(mtcr.getType())))).thenReturn(expectedTypeCount);
		AsyncMigrationTypeCountResult expectedAsyncRes = new AsyncMigrationTypeCountResult();
		expectedAsyncRes.setCount(expectedTypeCount);
		
		reqProcessor.processAsyncMigrationTypeCountRequest(mockProgressCallback, userInfo, mtcr, jobId);
		
		verify(mockAsynchJobStatusManager).setComplete(jobId, expectedAsyncRes);
		
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcessAsyncMigrationTypeCountRequestNotOK() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AsyncMigrationTypeCountRequest mtcr = new AsyncMigrationTypeCountRequest();
		mtcr.setType(MigrationType.ACCESS_APPROVAL.name());
		Exception expectedException = new IllegalArgumentException("SomeException");
		when(mockMigrationManager.getMigrationTypeCount(any(UserInfo.class), any(MigrationType.class))).thenThrow(expectedException);
		
		reqProcessor.processAsyncMigrationTypeCountRequest(mockProgressCallback, userInfo, mtcr, jobId);
		
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, eq(expectedException));
		
	}

	@Test
	public void testProcessAsyncMigrationRangeChecksumRequestOK() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AsyncMigrationRangeChecksumRequest mtcr = new AsyncMigrationRangeChecksumRequest();
		mtcr.setType(MigrationType.ACCESS_APPROVAL.name());
		mtcr.setMinId(0L);
		mtcr.setMaxId(100L);
		mtcr.setSalt("abcd");
		MigrationRangeChecksum expectedChecksum = new MigrationRangeChecksum();
		expectedChecksum.setChecksum("checksum");
		expectedChecksum.setMinid(0L);
		expectedChecksum.setMaxid(100L);
		expectedChecksum.setType(MigrationType.ACCESS_APPROVAL);
		when(mockMigrationManager.getChecksumForIdRange(eq(userInfo), eq(MigrationType.ACCESS_APPROVAL), eq("abcd"), eq(0L), eq(100L))).thenReturn(expectedChecksum);
		AsyncMigrationRangeChecksumResult expectedAsyncRes = new AsyncMigrationRangeChecksumResult();
		expectedAsyncRes.setChecksum(expectedChecksum);
		
		reqProcessor.processAsyncMigrationRangeChecksumRequest(mockProgressCallback, userInfo, mtcr, jobId);
		
		verify(mockAsynchJobStatusManager).setComplete(jobId, expectedAsyncRes);
		
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcessAsyncMigrationRangeChecksumRequestNotOK() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AsyncMigrationRangeChecksumRequest mtcr = new AsyncMigrationRangeChecksumRequest();
		mtcr.setType(MigrationType.ACCESS_APPROVAL.name());
		mtcr.setMinId(0L);
		mtcr.setMaxId(100L);
		mtcr.setSalt("abcd");
		Exception expectedException = new IllegalArgumentException("SomeException");
		when(mockMigrationManager.getChecksumForIdRange(any(UserInfo.class), any(MigrationType.class), anyString(), anyLong(), anyLong())).thenThrow(expectedException);
	
		reqProcessor.processAsyncMigrationRangeChecksumRequest(mockProgressCallback, userInfo, mtcr, jobId);
		
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, eq(expectedException));
		
	}

	@Test
	public void testProcessAsyncMigrationTypeChecksumRequestOK() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AsyncMigrationTypeChecksumRequest mtcr = new AsyncMigrationTypeChecksumRequest();
		mtcr.setType(MigrationType.ACCESS_APPROVAL.name());
		MigrationTypeChecksum expectedChecksum = new MigrationTypeChecksum();
		expectedChecksum.setChecksum("checksum");
		expectedChecksum.setType(MigrationType.ACCESS_APPROVAL);
		when(mockMigrationManager.getChecksumForType(eq(userInfo), eq(MigrationType.ACCESS_APPROVAL))).thenReturn(expectedChecksum);
		AsyncMigrationTypeChecksumResult expectedAsyncRes = new AsyncMigrationTypeChecksumResult();
		expectedAsyncRes.setChecksum(expectedChecksum);
		
		reqProcessor.processAsyncMigrationTypeChecksumRequest(mockProgressCallback, userInfo, mtcr, jobId);
		
		verify(mockAsynchJobStatusManager).setComplete(jobId, expectedAsyncRes);
		
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcessAsyncMigrationTypeChecksumRequestNotOK() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AsyncMigrationTypeChecksumRequest mtcr = new AsyncMigrationTypeChecksumRequest();
		mtcr.setType(MigrationType.ACCESS_APPROVAL.name());
		Exception expectedException = new IllegalArgumentException("SomeException");
		when(mockMigrationManager.getChecksumForType(any(UserInfo.class), any(MigrationType.class))).thenThrow(expectedException);
	
		reqProcessor.processAsyncMigrationTypeChecksumRequest(mockProgressCallback, userInfo, mtcr, jobId);
		
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, eq(expectedException));
		
	}

	@Test
	public void testProcessAsyncMigrationRowMetadataRequestOK() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AsyncMigrationRowMetadataRequest mrmr = new AsyncMigrationRowMetadataRequest();
		mrmr.setType(MigrationType.ACCESS_APPROVAL.name());
		mrmr.setMinId(0L);
		mrmr.setMaxId(100L);
		mrmr.setLimit(100L);
		mrmr.setOffset(0L);
		RowMetadata rowMetadata = new RowMetadata();
		rowMetadata.setEtag("etag");
		rowMetadata.setId(200L);
		rowMetadata.setParentId(100L);
		List<RowMetadata> rowMetadataList = new LinkedList<RowMetadata>();
		rowMetadataList.add(rowMetadata);
		RowMetadataResult expectedRowMetadataRes = new RowMetadataResult();
		expectedRowMetadataRes.setTotalCount(1L);
		expectedRowMetadataRes.setList(rowMetadataList);
		when(mockMigrationManager.getRowMetadataByRangeForType(userInfo, MigrationType.ACCESS_APPROVAL, 0L, 100L, 100L, 0L)).thenReturn(expectedRowMetadataRes);
		AsyncMigrationRowMetadataResult expectedAsyncRes = new AsyncMigrationRowMetadataResult();
		expectedAsyncRes.setRowMetadata(expectedRowMetadataRes);;
		
		reqProcessor.processAsyncMigrationRowMetadataRequest(mockProgressCallback, userInfo, mrmr, jobId);
		
		verify(mockAsynchJobStatusManager).setComplete(jobId, expectedAsyncRes);
		
	}

	@Test(expected=IllegalArgumentException.class)
	public void testProcessAsyncMigrationRowMetadataRequestNotOK() throws Throwable {
		String jobId = "1";
		UserInfo userInfo = new UserInfo(true);
		userInfo.setId(100L);
		AsyncMigrationRowMetadataRequest mrmr = new AsyncMigrationRowMetadataRequest();
		mrmr.setType(MigrationType.ACCESS_APPROVAL.name());
		mrmr.setMinId(0L);
		mrmr.setMaxId(100L);
		mrmr.setLimit(100L);
		mrmr.setOffset(0L);
		Exception expectedException = new IllegalArgumentException("SomeException");
		when(mockMigrationManager.getRowMetadataByRangeForType(any(UserInfo.class), any(MigrationType.class), anyLong(), anyLong(), anyLong(), anyLong())).thenThrow(expectedException);
	
		reqProcessor.processAsyncMigrationRowMetadataRequest(mockProgressCallback, userInfo, mrmr, jobId);
		
		verify(mockAsynchJobStatusManager).setJobFailed(jobId, eq(expectedException));
		
	}

}
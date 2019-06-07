package org.sagebionetworks.search;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.util.ThreadLocalProvider;
import org.sagebionetworks.util.ValidateArgument;

import com.amazonaws.services.cloudsearchdomain.AmazonCloudSearchDomain;
import com.amazonaws.services.cloudsearchdomain.model.AmazonCloudSearchDomainException;
import com.amazonaws.services.cloudsearchdomain.model.DocumentServiceException;
import com.amazonaws.services.cloudsearchdomain.model.SearchException;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.amazonaws.services.cloudsearchdomain.model.UploadDocumentsRequest;
import com.google.common.collect.Iterators;

/**
 * A wrapper for AWS's AmazonCloudSearchDomain. DO NOT INSTANTIATE.
 * Use CloudSearchClientProvider to get an instance of this class.
 */
public class CloudsSearchDomainClientAdapter {
	static private Logger logger = LogManager.getLogger(CloudsSearchDomainClientAdapter.class);

	static ThreadLocal<Map<Long, CloudSearchDocumentGenerationAwsKinesisLogRecord>> threadLocalRecordMap =
			ThreadLocalProvider.getInstanceWithInitial(CloudSearchDocumentGenerationAwsKinesisLogRecord.KINESIS_DATA_STREAM_NAME_SUFFIX, HashMap::new);


	private final AmazonCloudSearchDomain client;
	private final CloudSearchDocumentBatchIteratorProvider iteratorProvider;

	//used for logging ifo about a batch of documents
	private final AwsKinesisFirehoseLogger firehoseLogger;

	CloudsSearchDomainClientAdapter(AmazonCloudSearchDomain client, CloudSearchDocumentBatchIteratorProvider iteratorProvider, AwsKinesisFirehoseLogger firehoseLogger){
		this.client = client;
		this.iteratorProvider = iteratorProvider;
		this.firehoseLogger = firehoseLogger;
	}

	public void sendDocuments(Iterator<Document> documents){
		ValidateArgument.required(documents, "documents");
		Iterator<CloudSearchDocumentBatch> searchDocumentFileIterator = iteratorProvider.getIterator(documents);


		while(searchDocumentFileIterator.hasNext()) {
			Set<String> documentIds = null;
			try (CloudSearchDocumentBatch batch = searchDocumentFileIterator.next();
				 InputStream fileStream = batch.getNewInputStream();) {

				documentIds = batch.getDocumentIds();

				UploadDocumentsRequest request = new UploadDocumentsRequest()
						.withContentType("application/json")
						.withDocuments(fileStream)
						.withContentLength(batch.size());
				UploadDocumentsResult result = client.uploadDocuments(request);
				updateAndSendKinesisLogRecords(result.getStatus());
			} catch (DocumentServiceException e) {
				logger.error("The following documents failed to upload: " +  documentIds);
				documentIds = null;
				updateAndSendKinesisLogRecords(e.getStatus());
				throw handleCloudSearchExceptions(e);
			} catch (IOException e){
				throw new TemporarilyUnavailableException(e);
			}
		}
	}

	void updateAndSendKinesisLogRecords(final String status){
		final long batchUploadTimestamp = System.currentTimeMillis();
		final String batchUUID = UUID.randomUUID().toString();

		//exit early if map is empty since kinesis does not allow pushing of empty messages
		if(threadLocalRecordMap.get().isEmpty()){
			return;
		}

		//add additional batch releated metadata to each record in the batch and push to kinesis
		Stream<AwsKinesisLogRecord> logRecordStream = threadLocalRecordMap.get().values().stream()
				.map((record) ->
					record.withDocumentBatchUpdateStatus(status)
							.withDocumentBatchUpdateTimestamp(batchUploadTimestamp)
							.withDocumentBatchUUID(batchUUID)
				);
		firehoseLogger.logBatch(CloudSearchDocumentGenerationAwsKinesisLogRecord.KINESIS_DATA_STREAM_NAME_SUFFIX, logRecordStream);

		//remove all records from the map now that they are processed
		threadLocalRecordMap.get().clear();
	}


	public void sendDocument(Document document){
		ValidateArgument.required(document, "document");
		sendDocuments(Iterators.singletonIterator(document));
	}

	SearchResult rawSearch(SearchRequest request) {
		ValidateArgument.required(request, "request");

		try{
			return client.search(request);
		}catch (SearchException e){
			throw handleCloudSearchExceptions(e);
		}
	}

	RuntimeException handleCloudSearchExceptions(AmazonCloudSearchDomainException e){
		int statusCode = e.getStatusCode();
		if(statusCode / 100 == 4){ //4xx status codes
			return new IllegalArgumentException(e);
		} else if (statusCode / 100 == 5){ // 5xx status codes
			// The AWS API already has retry logic for 5xx status codes so getting one here means retries failed
			// AmazonCloudSearchDomainException is a subclass of AmazonServiceException,
			// which is already handled by BaseController and mapped to a 502 HTTP error
			return e;
		}else {
			logger.warn("Failed for unexpected reasons with status: " + e.getStatusCode());
			return e;
		}
	}

}

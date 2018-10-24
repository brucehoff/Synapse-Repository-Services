package org.sagebionetworks.search;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.output.CountingOutputStream;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.ValidateArgument;

public class CloudSearchDocumentBatchBuilder implements Closeable {

	private static final long MEBIBYTE = 1048576; // CloudSearch's Limits say MB but they really mean MiB
	private static final long DEFAULT_MAX_SINGLE_DOCUMENT_SIZE = MEBIBYTE; //1MiB
	private static final long DEFAULT_MAX_DOCUMENT_BATCH_SIZE = 5 * MEBIBYTE; //5MiB

	static final Charset CHARSET = StandardCharsets.UTF_8;
	static final byte[] PREFIX_BYTES = "[".getBytes(CHARSET);
	static final byte[] SUFFIX_BYTES = "]".getBytes(CHARSET);
	static final byte[] DELIMITER_BYTES = ",".getBytes(CHARSET);

	File documentBatchFile;

	CountingOutputStream countingOutputStream;

	Set<String> documentIds;

	//maximum bytes
	private final long maxSingleDocumentSizeInBytes;
	private final long maxDocumentBatchSizeInBytes;

	private CloudSearchDocumentBatch builtBatch;

	public CloudSearchDocumentBatchBuilder(FileProvider fileProvider) throws IOException {
		this(fileProvider, DEFAULT_MAX_SINGLE_DOCUMENT_SIZE, DEFAULT_MAX_DOCUMENT_BATCH_SIZE);
	}

	public CloudSearchDocumentBatchBuilder(FileProvider fileProvider, long maxSingleDocumentSizeInBytes, long maxDocumentBatchSizeInBytes) throws IOException{
		if (maxSingleDocumentSizeInBytes + PREFIX_BYTES.length + SUFFIX_BYTES.length > maxDocumentBatchSizeInBytes){
			throw new IllegalArgumentException("maxSingleDocumentSizeInBytes + " + (PREFIX_BYTES.length + SUFFIX_BYTES.length) + " must be less or equal to than maxDocumentBatchSizeInBytes");
		}
		try {
			this.builtBatch = null;


			this.documentBatchFile = fileProvider.createTempFile("CloudSearchDocument", ".json");
			this.countingOutputStream = new CountingOutputStream(fileProvider.createFileOutputStream(documentBatchFile));
			this.documentIds = new HashSet<>();

			this.maxSingleDocumentSizeInBytes = maxSingleDocumentSizeInBytes;
			this.maxDocumentBatchSizeInBytes = maxDocumentBatchSizeInBytes;

			//initialize the file with prefix
			this.countingOutputStream.write(PREFIX_BYTES);
		} catch (Exception e){
			close();
			throw e;
		}
	}

	/**
	 * Build the CloudSearchDocumentBatch
	 * @return the built documentBatch
	 * @throws IOException
	 */
	public CloudSearchDocumentBatch build() throws IOException {
		if(builtBatch != null){
			return builtBatch;
		}

		//append suffix
		countingOutputStream.write(SUFFIX_BYTES);
		countingOutputStream.flush();
		countingOutputStream.close();

		builtBatch = new CloudSearchDocumentBatchImpl(documentBatchFile, documentIds, countingOutputStream.getByteCount());
		return builtBatch;
	}

	/**
	 * Attempt to add document to the batch. Returns true if bytes could be added, false otherwise. Once false is returned,
	 * {@link #build()} should be called.
	 * @return true if bytes could be added, false otherwise
	 */
	public boolean tryAddDocument(Document document) throws IOException {
		ValidateArgument.required(document, "document");
		ValidateArgument.required(document.getId(), "document.Id");

		if (builtBatch != null){ //If it's already been built, don't add document
			return false;
		}

		byte[] documentBytes = SearchUtil.convertSearchDocumentToJSONString(document).getBytes(CHARSET);

		//if a single document exceeds the single document size limit throw exception
		if (documentBytes.length > maxSingleDocumentSizeInBytes) {
			throw new IllegalArgumentException("The document for " + document.getId() + " is " + documentBytes.length + " bytes and exceeds the maximum allowed " + maxSingleDocumentSizeInBytes + " bytes.");
		}

		boolean isNotFirstDocument = countingOutputStream.getByteCount() > PREFIX_BYTES.length;

		//determine how many bytes need to be written
		//always reserve space for the suffix because the next document being added may not fit into this document batch.
		long bytesToBeAdded = documentBytes.length + SUFFIX_BYTES.length;
		if(isNotFirstDocument) {
			bytesToBeAdded += DELIMITER_BYTES.length;
		}

		// Check there is enough space to write into this batch
		if(countingOutputStream.getByteCount() + bytesToBeAdded > maxDocumentBatchSizeInBytes){
			return false;
		}

		//add delimiter bytes if necessary and document bytes for this file
		if(isNotFirstDocument){
			countingOutputStream.write(DELIMITER_BYTES);
		}
		countingOutputStream.write(documentBytes);

		documentIds.add(document.getId());
		return true;
	}

	@Override
	public void close() throws IOException {
		if(countingOutputStream != null){
			countingOutputStream.close();
		}

		if(builtBatch == null && documentBatchFile != null) {
			documentBatchFile.delete();
		}
	}
}
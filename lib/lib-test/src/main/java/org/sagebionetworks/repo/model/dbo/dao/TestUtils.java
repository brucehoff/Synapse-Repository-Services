package org.sagebionetworks.repo.model.dbo.dao;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;

import com.amazonaws.util.BinaryUtils;

public class TestUtils {

	/**
	 * Helper to create a S3FileHandle
	 * 
	 * @return
	 */
	public static S3FileHandle createS3FileHandle(String createdById, String fileHandleId) {
		return createS3FileHandle(createdById, 123, fileHandleId);
	}

	/**
	 * Helper to create a S3FileHandle
	 * 
	 * @return
	 */
	public static S3FileHandle createS3FileHandle(String createdById, int sizeInBytes, String fileHandleId) {
		return createS3FileHandle(createdById, sizeInBytes, "content type", fileHandleId);
	}

	/**
	 * Helper to create a S3FileHandle
	 * 
	 * @return
	 */
	public static S3FileHandle createS3FileHandle(String createdById, int sizeInBytes, String contentType, String fileHandleId) {
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType(contentType);
		meta.setContentSize((long)sizeInBytes);
		meta.setContentMd5("md5");
		meta.setCreatedBy(createdById);
		meta.setFileName("foobar.txt");
		meta.setId(fileHandleId);
		meta.setEtag(UUID.randomUUID().toString());
		return meta;
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static PreviewFileHandle createPreviewFileHandle(String createdById, String fileHandleId) {
		return createPreviewFileHandle(createdById, 123, fileHandleId);
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static PreviewFileHandle createPreviewFileHandle(String createdById, int sizeInBytes, String fileHandleId) {
		return createPreviewFileHandle(createdById, sizeInBytes, "content type", fileHandleId);
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static PreviewFileHandle createPreviewFileHandle(String createdById, int sizeInBytes, String contentType, String fileHandleId) {
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType(contentType);
		meta.setContentSize((long)sizeInBytes);
		meta.setContentMd5("md5");
		meta.setCreatedBy(createdById);
		meta.setFileName("preview.jpg");
		meta.setEtag(UUID.randomUUID().toString());
		meta.setId(fileHandleId);
		return meta;
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static ExternalFileHandle createExternalFileHandle(String createdById) {
		return createExternalFileHandle(createdById, "content type");
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public static ExternalFileHandle createExternalFileHandle(String createdById, String contentType) {
		ExternalFileHandle meta = new ExternalFileHandle();
		meta.setExternalURL("http://www.example.com/");
		meta.setContentType(contentType);
		meta.setCreatedBy(createdById);
		meta.setFileName("External");
		return meta;
	}

	/**
	 * Calculate the MD5 digest of a given string.
	 * @param tocalculate
	 * @return
	 */
	public static String calculateMD5(String tocalculate){
		try {
			MessageDigest digetst = MessageDigest.getInstance("MD5");
			byte[] bytes = digetst.digest(tocalculate.getBytes("UTF-8"));
			return  BinaryUtils.toHex(bytes);	
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}	
	
	/**
	 * Create a populated Annotations object.
	 * 
	 * @return
	 */
	public static Annotations createDummyAnnotations() {
		return createDummyAnnotations(1);
	}
	
	public static final String PUBLIC_STRING_ANNOTATION_NAME = "string_anno";
	public static final String PUBLIC_STRING_ANNOTATION_WITH_NULLS_NAME = "string anno_null";
	public static final String PRIVATE_LONG_ANNOTATION_NAME = "long_anno";
	public static final String PUBLIC_DOUBLE_ANNOTATION_NAME = "double_anno";

	public static Annotations createDummyAnnotations(int i) {
		return createDummyAnnotations(i, (double)(0.5d+i), (long)(i*10L));
	}
		
	public static Annotations createDummyAnnotations(int i, Double d, long l) {
		String objectId = ""+i;
		String scopeId = "" + 2*i;
		String s = "foo " + i;
		boolean includeSecondString = (i % 4 != 3);
		String s2Value = (i % 2 == 1) ? null : "not null"; // odd numbered annotations are null
		return createDummyAnnotations(objectId, scopeId, i, s, includeSecondString, s2Value, l, d);
	}
		
	public static Annotations createDummyAnnotations(String objectId, String scopeId, int i, String s, boolean includeSecondString, String s2Value, long l, Double d) {
		List<StringAnnotation> stringAnnos = new ArrayList<StringAnnotation>();
		StringAnnotation sa = new StringAnnotation();
		sa.setIsPrivate(false);
		sa.setKey(PUBLIC_STRING_ANNOTATION_NAME);
		sa.setValue(s);
		stringAnnos.add(sa);
		
		if (includeSecondString) { // two ways to have a null annot:  set it null or omit altogether
			StringAnnotation sa2 = new StringAnnotation();
			sa2.setIsPrivate(false);
			sa2.setKey(PUBLIC_STRING_ANNOTATION_WITH_NULLS_NAME);
			sa2.setValue(s2Value);
			stringAnnos.add(sa2);
		}
		
		List<LongAnnotation> longAnnos = new ArrayList<LongAnnotation>();
		LongAnnotation la = new LongAnnotation();
		la.setIsPrivate(true);
		la.setKey(PRIVATE_LONG_ANNOTATION_NAME);
		la.setValue(new Long(l));
		longAnnos.add(la);
		
		List<DoubleAnnotation> doubleAnnos = new ArrayList<DoubleAnnotation>();
		if (d!=null) {
			DoubleAnnotation da = new DoubleAnnotation();
			da.setIsPrivate(false);
			da.setKey(PUBLIC_DOUBLE_ANNOTATION_NAME);
			da.setValue(d);
			doubleAnnos.add(da);
		}
		
		Annotations annos = new Annotations();
		annos.setStringAnnos(stringAnnos);
		annos.setLongAnnos(longAnnos);
		annos.setDoubleAnnos(doubleAnnos);
		annos.setObjectId(objectId);
		annos.setScopeId(scopeId);
		return annos;
	}
	
	
}

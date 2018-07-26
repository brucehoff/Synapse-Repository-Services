package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.Timestamp;

import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.persistence.DBODoi;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.KeyFactory;

public class DoiUtils {

	/**
	 * Converts a DOI DBO into a Doi DTO
	 * @param dbo A DOI database object
	 * @return A DOI data transfer object with all fields filled in.
	 */
	public static Doi convertToDto(DBODoi dbo) {
		if (dbo == null) {
			throw new IllegalArgumentException("DBO cannot be null.");
		}
		Doi dto = new Doi();
		dto.setId(dbo.getId().toString());
		dto.setEtag(dbo.getETag());
		dto.setDoiStatus(DoiStatus.valueOf(dbo.getDoiStatus()));
		final ObjectType objectType = ObjectType.valueOf(dbo.getObjectType());
		if (ObjectType.ENTITY.equals(objectType)) {
			dto.setObjectId(KeyFactory.keyToString(dbo.getObjectId()));
		} else {
			dto.setObjectId(dbo.getObjectId().toString());
		}
		dto.setObjectType(objectType);
		if (dbo.getObjectVersion().equals(DBODoi.NULL_OBJECT_VERSION)) {
			dto.setObjectVersion(null);
		} else {
			dto.setObjectVersion(dbo.getObjectVersion());
		}
		dto.setCreatedBy(dbo.getCreatedBy().toString());
		dto.setCreatedOn(dbo.getCreatedOn());
		dto.setUpdatedOn(dbo.getUpdatedOn());
		return dto;
	}

	/**
	 * Converts a DOI DTO into a DOI DBO
	 * @param dto A DOI data transfer object to convert to a DBO
	 * @return A corresponding DOI database object.
	 */
	public static DBODoi convertToDbo(Doi dto) {
		if (dto == null) {
			throw new IllegalArgumentException("DTO cannot be null.");
		}
		DBODoi dbo = new DBODoi();
		if (dto.getId() != null) {
			dbo.setId(Long.valueOf(dto.getId()));
		}
		dbo.setETag(dto.getEtag());
		dbo.setDoiStatus(dto.getDoiStatus());
		dbo.setObjectId(KeyFactory.stringToKey(dto.getObjectId()));
		dbo.setObjectType(dto.getObjectType());
		if (dto.getObjectVersion() == null) {
			dbo.setObjectVersion(DBODoi.NULL_OBJECT_VERSION);
		} else {
			dbo.setObjectVersion(dto.getObjectVersion());
		}
		if (dto.getCreatedBy() != null) {
			dbo.setCreatedBy(Long.valueOf(dto.getCreatedBy()));
		}
		if (dto.getCreatedOn() != null) {
			dbo.setCreatedOn(new Timestamp(dto.getCreatedOn().getTime()));
		}
		if (dto.getUpdatedOn() != null) {
			dbo.setUpdatedOn(new Timestamp(dto.getUpdatedOn().getTime()));
		}
		return dbo;
	}
}

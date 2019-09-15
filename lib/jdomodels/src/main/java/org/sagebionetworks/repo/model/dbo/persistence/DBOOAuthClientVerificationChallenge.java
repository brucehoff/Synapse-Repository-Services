/**
 * 
 */
package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_IS_VERIFIED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_PROPERTIES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_SECRET_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_CLIENT_SECTOR_IDENTIFIER_URI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OAUTH_CLIENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_CLIENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * TODO this should be a secondary table to DBOOAuthClient
 */
public class DBOOAuthClientVerificationChallenge implements MigratableDatabaseObject<DBOOAuthClientVerificationChallenge, DBOOAuthClientVerificationChallenge> {
	private Long oauthClientId;
	private Long createdOn;
	private Long createdBy;
	private byte[] properties;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("oauthClientId", COL_OAUTH_CLIENT_ID, true).withIsBackupId(true),
		new FieldColumn("createdBy", COL_OAUTH_CLIENT_CREATED_BY),
		new FieldColumn("createdOn", COL_OAUTH_CLIENT_CREATED_ON),
		new FieldColumn("properties", COL_OAUTH_CLIENT_PROPERTIES),
		};

	@Override
	public TableMapping<DBOOAuthClientVerificationChallenge> getTableMapping() {
		return new TableMapping<DBOOAuthClientVerificationChallenge>() {
			// Map a result set to this object
			@Override
			public DBOOAuthClientVerificationChallenge mapRow(ResultSet rs, int rowNum)	throws SQLException {
				DBOOAuthClientVerificationChallenge client = new DBOOAuthClientVerificationChallenge();
				client.setId(rs.getLong(COL_OAUTH_CLIENT_ID));
				client.setCreatedOn(rs.getLong(COL_OAUTH_CLIENT_CREATED_ON));
				client.setCreatedBy(rs.getLong(COL_OAUTH_CLIENT_CREATED_BY));
				client.setProperties(rs.getBytes(COL_OAUTH_CLIENT_PROPERTIES));
				return client;
			}

			@Override
			public String getTableName() {
				return TABLE_OAUTH_CLIENT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_OAUTH_CLIENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOOAuthClientVerificationChallenge> getDBOClass() {
				return DBOOAuthClientVerificationChallenge.class;
			}
		};
	}


	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.OAUTH_CLIENT;
	}
	
	@Override
	public MigratableTableTranslation<DBOOAuthClientVerificationChallenge, DBOOAuthClientVerificationChallenge> getTranslator() {
			return new BasicMigratableTableTranslation<DBOOAuthClientVerificationChallenge>();
	}


	@Override
	public Class<? extends DBOOAuthClientVerificationChallenge> getBackupClass() {
		return DBOOAuthClientVerificationChallenge.class;
	}


	@Override
	public Class<? extends DBOOAuthClientVerificationChallenge> getDatabaseObjectClass() {
		return DBOOAuthClientVerificationChallenge.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}



}

package org.sagebionetworks.repo.model.dbo.dao.statistics;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_STATISTICS_MONTHLY_PROJECT_FILES;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyProjectDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.statistics.monthly.DBOMonthlyStatisticsProjectFiles;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyProjectFiles;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsMonthlyProjectFilesDAOImpl implements StatisticsMonthlyProjectDAO {

	private static final String PARAM_PROJECT_ID = "projectId";
	private static final String PARAM_MONTH = "month";
	private static final String PARAM_EVENT_TYPE = "eventType";
	private static final String PARAM_FROM = "from";
	private static final String PARAM_TO = "to";

	// @formatter:off

	private static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE_STATISTICS_MONTHLY_PROJECT_FILES;

	private static final String SQL_SELECT_IN_RANGE = "SELECT * FROM " 
			+ TABLE_STATISTICS_MONTHLY_PROJECT_FILES + " WHERE "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID + " = :" + PARAM_PROJECT_ID + " AND "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE + " =:" + PARAM_EVENT_TYPE + " AND " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH + " BETWEEN :" + PARAM_FROM + " AND :" + PARAM_TO 
			+ " ORDER BY " + COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH;
	
	private static final String SQL_COUNT_PROJECTS_IN_RANGE = "SELECT COUNT(DISTINCT " + COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID + ") FROM " 
			+ TABLE_STATISTICS_MONTHLY_PROJECT_FILES + " WHERE "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE + " =:" + PARAM_EVENT_TYPE + " AND " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH + " BETWEEN :" + PARAM_FROM + " AND :" + PARAM_TO 
			+ " ORDER BY " + COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH;

	private static final String SQL_SAVE_BATCH = "INSERT INTO " + TABLE_STATISTICS_MONTHLY_PROJECT_FILES 
			+ "(" + COL_STATISTICS_MONTHLY_PROJECT_FILES_PROJECT_ID + ", " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_MONTH + ", "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_EVENT_TYPE + ", " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT + ", "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT + ", " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON
			+ ") VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_FILES_COUNT + " = ?, "
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_USERS_COUNT + " = ?, " 
			+ COL_STATISTICS_MONTHLY_PROJECT_FILES_LAST_UPDATED_ON + " = ?";

	// @formatter:on

	private static final RowMapper<DBOMonthlyStatisticsProjectFiles> DBO_MAPPER = new DBOMonthlyStatisticsProjectFiles().getTableMapping();

	private static final RowMapper<StatisticsMonthlyProjectFiles> ROW_MAPPER = new RowMapper<StatisticsMonthlyProjectFiles>() {
		@Override
		public StatisticsMonthlyProjectFiles mapRow(ResultSet rs, int rowNum) throws SQLException {
			return map(DBO_MAPPER.mapRow(rs, rowNum));
		}
	};

	private DBOBasicDao basicDao;
	private NamedParameterJdbcTemplate jdbcTemplate;

	@Autowired
	public StatisticsMonthlyProjectFilesDAOImpl(DBOBasicDao basicDao, NamedParameterJdbcTemplate jdbcTemplate) {
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<StatisticsMonthlyProjectFiles> getProjectFilesStatisticsInRange(Long projectId, FileEvent eventType, YearMonth from,
			YearMonth to) {
		ValidateArgument.required(projectId, "projectId");
		ValidateArgument.required(eventType, "eventType");
		ValidateArgument.required(from, "from");
		ValidateArgument.required(to, "to");
		ValidateArgument.requirement(from.equals(to) || from.isBefore(to), "The start of the range should be before the end");

		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_PROJECT_ID, projectId);
		params.addValue(PARAM_EVENT_TYPE, eventType.toString());
		params.addValue(PARAM_FROM, StatisticsMonthlyUtils.toDate(from));
		params.addValue(PARAM_TO, StatisticsMonthlyUtils.toDate(to));

		return jdbcTemplate.query(SQL_SELECT_IN_RANGE, params, ROW_MAPPER);
	}

	@Override
	public Long countProjectsInRange(FileEvent eventType, YearMonth from, YearMonth to) {
		ValidateArgument.required(eventType, "eventType");
		ValidateArgument.required(from, "from");
		ValidateArgument.required(to, "to");
		ValidateArgument.requirement(from.equals(to) || from.isBefore(to), "The start of the range should be before the end");

		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_EVENT_TYPE, eventType.toString());
		params.addValue(PARAM_FROM, StatisticsMonthlyUtils.toDate(from));
		params.addValue(PARAM_TO, StatisticsMonthlyUtils.toDate(to));

		return jdbcTemplate.queryForObject(SQL_COUNT_PROJECTS_IN_RANGE, params, Long.class);
	}

	@Override
	public Optional<StatisticsMonthlyProjectFiles> getProjectFilesStatistics(Long projectId, FileEvent eventType, YearMonth month) {
		ValidateArgument.required(projectId, "projectId");
		ValidateArgument.required(eventType, "eventType");
		ValidateArgument.required(month, "month");

		SqlParameterSource params = getPrimaryKeyParams(projectId, month, eventType);

		DBOMonthlyStatisticsProjectFiles dbo = basicDao.getObjectByPrimaryKeyIfExists(DBOMonthlyStatisticsProjectFiles.class, params);

		if (dbo == null) {
			return Optional.empty();
		}

		return Optional.of(map(dbo));
	}

	@Override
	@WriteTransaction
	public void save(List<StatisticsMonthlyProjectFiles> batch) {
		ValidateArgument.required(batch, "batch");
		
		if (batch.isEmpty()) {
			return;
		}

		// Gets an instance of the underlying JdbcTemplate to perform batch operations

		JdbcTemplate template = jdbcTemplate.getJdbcTemplate();

		template.batchUpdate(SQL_SAVE_BATCH, new BatchPreparedStatementSetter() {

			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				StatisticsMonthlyProjectFiles dto = batch.get(i);

				int index = 1;

				long now = System.currentTimeMillis();

				// On create fields
				ps.setLong(index++, dto.getProjectId());
				ps.setObject(index++, StatisticsMonthlyUtils.toDate(dto.getMonth()));
				ps.setString(index++, dto.getEventType().toString());
				ps.setInt(index++, dto.getFilesCount());
				ps.setInt(index++, dto.getUsersCount());
				ps.setLong(index++, now);

				// On duplicate update fields
				ps.setInt(index++, dto.getFilesCount());
				ps.setInt(index++, dto.getUsersCount());
				ps.setLong(index++, now);

			}

			@Override
			public int getBatchSize() {
				return batch.size();
			}
		});

	}

	@Override
	@WriteTransaction
	public void clear() {
		jdbcTemplate.update(SQL_DELETE_ALL, EmptySqlParameterSource.INSTANCE);
	}

	private MapSqlParameterSource getPrimaryKeyParams(Long projectId, YearMonth month, FileEvent eventType) {
		MapSqlParameterSource params = new MapSqlParameterSource();

		params.addValue(PARAM_PROJECT_ID, projectId);
		params.addValue(PARAM_MONTH, StatisticsMonthlyUtils.toDate(month));
		params.addValue(PARAM_EVENT_TYPE, eventType.toString());

		return params;
	}

	private static StatisticsMonthlyProjectFiles map(DBOMonthlyStatisticsProjectFiles dbo) {
		StatisticsMonthlyProjectFiles dto = new StatisticsMonthlyProjectFiles();

		dto.setProjectId(dbo.getProjectId());
		dto.setMonth(YearMonth.from(dbo.getMonth()));
		dto.setEventType(FileEvent.valueOf(dbo.getEventType()));
		dto.setFilesCount(dbo.getFilesCount());
		dto.setUsersCount(dbo.getUsersCount());
		dto.setLastUpdatedOn(dbo.getLastUpdatedOn());

		return dto;
	}

}

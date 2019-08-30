package org.sagebionetworks.repo.model.dao.statistics;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.statistics.StatisticsMonthlyStatus;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;

public interface StatisticsMonthlyStatusDAO {

	/**
	 * Sets the status for the given object type and month to {@link StatisticsStatus#AVAILABLE}, sets
	 * the lastSucceededAt timestamp. Creates a new record if it does not exist.
	 * 
	 * @param objectType
	 * @param month
	 * @return
	 */
	StatisticsMonthlyStatus setAvailable(StatisticsObjectType objectType, YearMonth month);

	/**
	 * Sets the status for the given object type and month to
	 * {@link StatisticsStatus#PROCESSING_FAILED}, sets the lastFailedAt timestamp. Creates a new record
	 * if it does not exist.
	 * 
	 * @param objectType
	 * @param month
	 * @return
	 */
	StatisticsMonthlyStatus setProcessingFailed(StatisticsObjectType objectType, YearMonth month);

	/**
	 * Sets the status for the given object type and month to {@link StatisticsStatus#PROCESSING}, sets
	 * the lastStartedAt timestamp. Creates a new record if it does not exist.
	 * 
	 * @param objectType
	 * @param month
	 * @return
	 */
	StatisticsMonthlyStatus setProcessing(StatisticsObjectType objectType, YearMonth month);

	/**
	 * Retrieve the monthly statistics status for the given month.
	 * 
	 * @param objectType The type of object
	 * @param month      The considered month
	 * @return An optional containing the statistics status for the given type and month if present,
	 *         empty otherwise
	 */
	Optional<StatisticsMonthlyStatus> getStatus(StatisticsObjectType objectType, YearMonth month);

	/**
	 * Return the list of statuses that are set to available for the given object in the given range
	 * [from, to].
	 * 
	 * @param objectType The statistics object type
	 * @param from       The start of the range (inclusive)
	 * @param to         The end of the range (exclusive)
	 * @return The list of statuses for the given object in the given range [from, to]
	 */
	List<StatisticsMonthlyStatus> getAvailableStatusInRange(StatisticsObjectType objectType, YearMonth from, YearMonth to);

	/**
	 * Removes all the statuses
	 */
	void clear();

}

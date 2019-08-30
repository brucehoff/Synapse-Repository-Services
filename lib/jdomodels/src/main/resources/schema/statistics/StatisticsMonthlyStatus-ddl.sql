CREATE TABLE `STATISTICS_MONTHLY_STATUS` (
  `OBJECT_TYPE` enum('PROJECT') NOT NULL,
  `MONTH` DATE NOT NULL,
  `STATUS` enum('AVAILABLE','PROCESSING','PROCESSING_FAILED') NOT NULL,
  `LAST_STARTED_AT` bigint(20) DEFAULT NULL,
  `LAST_SUCCEEDED_AT` bigint(20) DEFAULT NULL,
  `LAST_FAILED_AT` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`OBJECT_TYPE`, `MONTH`)
);
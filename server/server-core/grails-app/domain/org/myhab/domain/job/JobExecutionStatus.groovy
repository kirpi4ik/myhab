package org.myhab.domain.job

/**
 * Enum representing the execution status of a Quartz job.
 */
enum JobExecutionStatus {

    /**
     * Job execution has started but not yet completed
     */
    STARTED,

    /**
     * Job execution completed successfully
     */
    COMPLETED,

    /**
     * Job execution failed with an exception
     */
    FAILED,

    /**
     * Job execution was vetoed by a trigger listener
     */
    VETOED
}

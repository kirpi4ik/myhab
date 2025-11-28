package org.myhab.domain

import org.myhab.domain.job.Job
import grails.gorm.DetachedCriteria

/**
 * Join table entity for the many-to-many relationship between User and their favorite Jobs.
 * 
 * This explicit join table provides better control over the relationship and
 * allows for easier querying and management of user favorite jobs.
 */
class UserFavJobJoin implements Serializable {

    User user
    Job job

    static mapping = {
        id composite: ['user', 'job']
        table 'users_fav_jobs'
        version false
    }

    /**
     * Get a specific user-job favorite relationship
     * 
     * @param userId The user ID
     * @param jobId The job ID
     * @return UserFavJobJoin instance or null if not found
     */
    static UserFavJobJoin get(long userId, long jobId) {
        criteriaFor(userId, jobId).get()
    }

    /**
     * Check if a user-job favorite relationship exists
     * 
     * @param userId The user ID
     * @param jobId The job ID
     * @return true if the relationship exists, false otherwise
     */
    static boolean exists(long userId, long jobId) {
        criteriaFor(userId, jobId).count() > 0
    }

    /**
     * Remove all favorite job entries for a specific job
     * 
     * @param jobId The job ID to remove from all users' favorites
     * @return Number of records deleted
     */
    static int removeAllForJob(long jobId) {
        // Load the job entity to use in the where clause (avoids join)
        Job jobEntity = Job.load(jobId)
        UserFavJobJoin.where {
            job == jobEntity
        }.deleteAll()
    }

    /**
     * Remove all favorite job entries for a specific user
     * 
     * @param userId The user ID
     * @return Number of records deleted
     */
    static int removeAllForUser(long userId) {
        // Load the user entity to use in the where clause (avoids join)
        User userEntity = User.load(userId)
        UserFavJobJoin.where {
            user == userEntity
        }.deleteAll()
    }

    private static DetachedCriteria criteriaFor(long userId, long jobId) {
        UserFavJobJoin.where {
            user == User.load(userId) && job == Job.load(jobId)
        }
    }
}


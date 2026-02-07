import {gql} from '@apollo/client/core';

export const JOB_LIST_ALL = gql`
  query {
    jobList {
      id
      name
      description
      state
      tsCreated
      tsUpdated
      scenario {
        id
        name
      }
    }
    scenarioList {
      id
      name
    }
    jobTagList {
      id
      name
    }
  }
`;

export const JOB_GET_BY_ID = gql`
  query ($id: Long!) {
    job(id: $id) {
      id
      name
      description
      state
      cronTriggers {
        id
        expression
        description
      }
      eventTriggers {
        id
        events {
          id
          name
        }
      }
      tags {
        id
        name
      }
      tsCreated
      tsUpdated
      scenario {
        id
        name
        body
      }
    }
  }
`;

export const JOB_EDIT_GET_BY_ID = gql`
  query ($id: Long!) {
    job(id: $id) {
      id
      name
      description
      state
      cronTriggers {
        id
        expression
        description
      }
      eventTriggers {
        id
        events {
          id
          name
        }
      }
      tags {
        id
        name
      }
      scenario {
        id
        name
      }
    }
    scenarioList {
      id
      name
    }
    jobTagList {
      id
      name
    }
    eventDefinitionList {
      id
      name
    }
  }
`;

export const JOB_CREATE = gql`
  mutation jobCreate($job: JobCreate) {
    jobCreate(job: $job) {
      id
    }
  }
`;

export const JOB_UPDATE = gql`
  mutation jobUpdate($id: Long!, $job: JobUpdate!) {
    jobUpdate(id: $id, job: $job) {
      id
    }
  }
`;

export const JOB_DELETE_BY_ID = gql`
  mutation ($id: Long!) {
    jobDeleteCascade(id: $id) {
      error
      success
    }
  }
`;

export const JOB_SCHEDULE = gql`
  mutation jobSchedule($jobId: ID!) {
    jobSchedule(jobId: $jobId) {
      success
      error
    }
  }
`;

export const JOB_UNSCHEDULE = gql`
  mutation jobUnschedule($jobId: ID!) {
    jobUnschedule(jobId: $jobId) {
      success
      error
    }
  }
`;

export const JOB_TRIGGER = gql`
  mutation jobTrigger($jobId: ID!) {
    jobTrigger(jobId: $jobId) {
      success
      error
    }
  }
`;

export const JOB_EXECUTION_HISTORY = gql`
  query jobExecutionHistoryByJobId($jobId: ID!, $limit: Int) {
    jobExecutionHistoryByJobId(jobId: $jobId, limit: $limit) {
      id
      jobId
      jobName
      jobGroup
      triggerName
      triggerGroup
      fireInstanceId
      startTime
      endTime
      durationMs
      status
      errorMessage
      exceptionClass
      recovering
      refireCount
      scheduledFireTime
      actualFireTime
      tsCreated
    }
  }
`;


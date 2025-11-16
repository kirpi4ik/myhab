import {gql} from '@apollo/client/core';

export const SCENARIO_LIST_ALL = gql`
  query {
    scenarioList {
      id
      name
      body
      tsCreated
      tsUpdated
    }
    devicePortList {
      id
      name
      internalRef
      device {
        id
        name
        code
      }
    }
  }
`;

export const SCENARIO_GET_BY_ID = gql`
  query ($id: Long!) {
    scenario(id: $id) {
      id
      name
      body
      tsCreated
      tsUpdated
      ports {
        id
        name
        internalRef
        device {
          id
          name
          code
        }
      }
    }
  }
`;

export const SCENARIO_EDIT_GET_BY_ID = gql`
  query ($id: Long!) {
    scenario(id: $id) {
      id
      name
      body
      ports {
        id
        name
        internalRef
        device {
          id
          name
          code
        }
      }
    }
    devicePortList {
      id
      name
      internalRef
      device {
        id
        name
        code
      }
    }
  }
`;

export const SCENARIO_CREATE = gql`
  mutation ($scenario: ScenarioCreate) {
    scenarioCreate(scenario: $scenario) {
      id
    }
  }
`;

export const SCENARIO_UPDATE = gql`
  mutation ($id: Long!, $scenario: ScenarioUpdate!) {
    updateScenario(id: $id, scenario: $scenario) {
      id
    }
  }
`;

export const SCENARIO_DELETE_BY_ID = gql`
  mutation ($id: ID!) {
    scenarioDelete(id: $id) {
      error
      success
    }
  }
`;


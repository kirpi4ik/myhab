import { gql } from '@apollo/client/core';

export const MQTT_PUBLISH = gql`
  mutation publishMqtt($topic: String!, $payload: String!) {
    publishMqtt(topic: $topic, payload: $payload) {
      success
      error
    }
  }
`;

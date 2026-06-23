import {gql} from '@apollo/client/core';

/**
 * Send a transcribed phrase to the backend, which resolves it (via the LLM)
 * to a peripheral + action and executes it. Returns the resolved entity so the
 * UI can confirm what happened.
 */
export const VOICE_COMMAND = gql`
  mutation voiceCommand($transcript: String!, $locale: String) {
    voiceCommand(transcript: $transcript, locale: $locale) {
      success
      error
      transcript
      action
      peripheralId
      peripheralName
      spokenResponse
    }
  }
`;

import {gql} from '@apollo/client/core';

/**
 * Query the global QR-code label settings.
 */
export const QR_CONFIG_GET = gql`
  query qrConfig {
    qrConfig {
      enabled
      contentTemplate
      position
      size
      availableVariables
    }
  }
`;

/**
 * Update the global QR-code label settings (commits to the git-backed config).
 */
export const QR_CONFIG_UPDATE = gql`
  mutation qrConfigUpdate($enabled: Boolean!, $contentTemplate: String!, $position: String!, $size: Int) {
    qrConfigUpdate(enabled: $enabled, contentTemplate: $contentTemplate, position: $position, size: $size) {
      success
      error
    }
  }
`;

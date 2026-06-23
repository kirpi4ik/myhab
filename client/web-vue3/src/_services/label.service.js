import { Utils } from '@/_helpers';
import { authzService } from '@/_services';

/**
 * Fetch a label PNG from the REST endpoint and trigger a browser download.
 * Shared by the per-entity helpers below.
 *
 * @param {string} kind - entity slug in the URL/filename ('cable', 'device', 'peripheral')
 * @param {number} id - entity ID
 * @param {object} options - {template, fields[], width, height}
 */
async function downloadEntityLabel(kind, id, options = {}) {
    const params = new URLSearchParams();

    if (options.template) params.append('template', options.template);
    if (options.fields) params.append('fields', options.fields.join(','));
    if (options.width) params.append('width', options.width);
    if (options.height) params.append('height', options.height);
    params.append('download', 'true');

    const url = `${Utils.host()}/api/labels/${kind}/${id}?${params.toString()}`;

    const response = await fetch(url, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${authzService.currentUserValue?.access_token || ''}`
        }
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const err = new Error(errorData.error || 'Failed to generate label');
        console.error('Error downloading label:', err);
        throw err;
    }

    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = `${kind}-${id}-label.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);

    return true;
}

/**
 * Service for downloading printable labels.
 * Backed by REST endpoints GET /api/labels/{kind}/{id} (binary PNG, not GraphQL — see LabelController).
 * QR inclusion is driven by the global feature.qr.enabled toggle on the server.
 */
export const labelService = {
    /** Download a cable label. options: {template, fields[], width, height} */
    downloadCableLabel(cableId, options = {}) {
        return downloadEntityLabel('cable', cableId, options);
    },

    /** Download a device label. options: {template, fields[], width, height} */
    downloadDeviceLabel(deviceId, options = {}) {
        return downloadEntityLabel('device', deviceId, options);
    },

    /** Download a peripheral label. options: {template, fields[], width, height} */
    downloadPeripheralLabel(peripheralId, options = {}) {
        return downloadEntityLabel('peripheral', peripheralId, options);
    }
};

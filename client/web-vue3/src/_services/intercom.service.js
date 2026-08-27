import { Utils } from '@/_helpers';
import { authzService } from '@/_services';

/**
 * REST client for intercom media (mirrors screen.service.js). The bytes/JSON come
 * from the backend IntercomController, which proxies the Tuya-Cloud helper under
 * JWT. A 204 means "not configured / nothing available" — the widget then shows a
 * placeholder rather than an error.
 */

function authHeaders() {
	const token = authzService.currentUserValue?.access_token || authzService.currentUserValue?.token;
	return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * Fetch a snapshot and return a blob URL for <img>. Caller must
 * URL.revokeObjectURL() on unmount/refetch.
 * @param {string|number} peripheralId
 * @param {{live?: boolean}} [opts] - live:true forces a fresh Tuya-Cloud frame grab
 * @returns {Promise<string|null>} blob URL, or null when nothing is available
 */
export async function fetchSnapshotBlobUrl(peripheralId, { live = false } = {}) {
	const params = new URLSearchParams({ t: String(Date.now()) });
	if (live) params.set('live', '1');
	const url = `${Utils.host()}/api/intercom/${peripheralId}/snapshot?${params.toString()}`;
	try {
		const response = await fetch(url, { method: 'GET', headers: authHeaders() });
		if (!response.ok || response.status === 204) return null;
		const blob = await response.blob();
		return URL.createObjectURL(blob);
	} catch {
		return null;
	}
}

/**
 * Fetch the live-stream descriptor for the video popup.
 * @param {string|number} peripheralId
 * @returns {Promise<{hls: string, expires: number}|null>}
 */
export async function fetchStreamUrl(peripheralId) {
	const url = `${Utils.host()}/api/intercom/${peripheralId}/stream`;
	try {
		const response = await fetch(url, { method: 'GET', headers: authHeaders() });
		if (!response.ok || response.status === 204) return null;
		return await response.json();
	} catch {
		return null;
	}
}

export const intercomService = {
	fetchSnapshotBlobUrl,
	fetchStreamUrl,
};

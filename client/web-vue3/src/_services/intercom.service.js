import { Utils } from '@/_helpers';
import { authzService } from '@/_services';

/**
 * REST client for intercom media. Snapshot bytes come from the backend
 * IntercomController (which proxies go2rtc's real frame under JWT); the live
 * stream is go2rtc's HLS, also proxied — hls.js loads the playlist URL and adds
 * the Bearer via xhrSetup so every playlist/segment request is authenticated.
 * A 204 means "not configured", so the widget shows a placeholder.
 */

function authHeaders() {
	const token = authToken();
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export function authToken() {
	return authzService.currentUserValue?.access_token || authzService.currentUserValue?.token;
}

/**
 * Fetch a snapshot and return a blob URL for <img>. Caller must
 * URL.revokeObjectURL() on unmount/refetch.
 * @param {string|number} peripheralId
 * @returns {Promise<string|null>} blob URL, or null when nothing is available
 */
export async function fetchSnapshotBlobUrl(peripheralId) {
	const url = `${Utils.host()}/api/intercom/${peripheralId}/snapshot?t=${Date.now()}`;
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
 * The HLS playlist URL for the live-video popup (loaded by hls.js).
 * @param {string|number} peripheralId
 * @returns {string}
 */
export function streamPlaylistUrl(peripheralId) {
	return `${Utils.host()}/api/intercom/${peripheralId}/stream.m3u8`;
}

export const intercomService = {
	fetchSnapshotBlobUrl,
	streamPlaylistUrl,
	authToken,
};

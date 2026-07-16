import { Utils } from '@/_helpers';
import { authzService } from '@/_services';

/**
 * REST client for dashboard screen binary content (mirrors avatar.service.js).
 * Screen metadata/layout go through GraphQL; only the background image bytes
 * and the legacy-SVG import use REST (binary streams).
 */

function authHeaders() {
	const token = authzService.currentUserValue?.access_token || authzService.currentUserValue?.token;
	return token ? { Authorization: `Bearer ${token}` } : {};
}

/**
 * @param {string|number} screenId
 * @returns {string} Background endpoint URL
 */
export function getBackgroundUrl(screenId) {
	return `${Utils.host()}/api/screens/${screenId}/background`;
}

/**
 * Fetch the screen background with Bearer auth and return a blob URL for
 * <image href>. Caller must URL.revokeObjectURL() on unmount/refetch.
 *
 * @param {string|number} screenId
 * @param {string|number} [version] - cache-buster (e.g. tsUpdated)
 * @returns {Promise<string|null>} Blob URL, or null when no background/error
 */
export async function fetchBackgroundBlobUrl(screenId, version) {
	const url = getBackgroundUrl(screenId) + (version ? `?v=${encodeURIComponent(version)}` : '');
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
 * Upload/replace a screen background image (admin).
 * @param {string|number} screenId
 * @param {File} file
 * @param {string} [preset] - 'mobile' | 'tablet' — contain-fit onto a fixed canvas;
 *                            omit to keep the image's natural dimensions
 * @returns {Promise<{success: boolean, width?: number, height?: number, error?: string}>}
 */
export async function uploadBackground(screenId, file, preset) {
	const form = new FormData();
	form.append('file', file);
	const url = getBackgroundUrl(screenId) + (preset ? `?preset=${encodeURIComponent(preset)}` : '');
	const response = await fetch(url, {
		method: 'PUT',
		headers: authHeaders(),
		body: form,
	});
	const data = await response.json().catch(() => ({}));
	if (!response.ok) throw new Error(data.error || `Upload failed (${response.status})`);
	return data;
}

/**
 * Re-fit the already-stored background onto a preset canvas (admin) — no
 * re-upload. The server also rescales the saved layout proportionally.
 * @param {string|number} screenId
 * @param {string} preset - 'mobile' | 'tablet'
 * @returns {Promise<{success: boolean, width?: number, height?: number, error?: string}>}
 */
export async function resizeBackground(screenId, preset) {
	const url = `${getBackgroundUrl(screenId)}/resize?preset=${encodeURIComponent(preset)}`;
	const response = await fetch(url, { method: 'POST', headers: authHeaders() });
	const data = await response.json().catch(() => ({}));
	if (!response.ok) throw new Error(data.error || `Resize failed (${response.status})`);
	return data;
}

/**
 * Import a legacy /wui SVG (admin). Creates a disabled screen with seeded widgets.
 * @param {File} file - the .svg file
 * @param {string} name - new screen name
 * @returns {Promise<{success: boolean, id: number, widgets: number}>}
 */
export async function importLegacySvg(file, name) {
	const form = new FormData();
	form.append('file', file);
	form.append('name', name);
	const response = await fetch(`${Utils.host()}/api/screens/import-svg`, {
		method: 'POST',
		headers: authHeaders(),
		body: form,
	});
	const data = await response.json().catch(() => ({}));
	if (!response.ok) throw new Error(data.error || `Import failed (${response.status})`);
	return data;
}

export const screenService = {
	getBackgroundUrl,
	fetchBackgroundBlobUrl,
	uploadBackground,
	resizeBackground,
	importLegacySvg,
};

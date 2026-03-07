import { Utils } from '@/_helpers';
import { authzService } from '@/_services';

/**
 * Avatar REST API URL and authenticated fetch for user avatars.
 * Avatars are served via GET /api/users/:id/avatar (Bearer auth).
 */

/**
 * @param {string|number} userId - User ID
 * @returns {string} Full URL for the user's avatar API endpoint
 */
export function getAvatarApiUrl(userId) {
	if (userId == null || userId === '') return '';
	return `${Utils.host()}/api/users/${userId}/avatar`;
}

/**
 * Fetches the user avatar with Bearer token and returns a blob URL for use in img src.
 * The caller should call URL.revokeObjectURL(url) when done (e.g. on unmount) to avoid leaks.
 *
 * @param {string|number} userId - User ID
 * @returns {Promise<string|null>} Blob URL, or null if no avatar / error
 */
export async function fetchAvatarBlobUrl(userId) {
	const url = getAvatarApiUrl(userId);
	if (!url) return null;
	const token = authzService.currentUserValue?.access_token || authzService.currentUserValue?.token;
	try {
		const response = await fetch(url, {
			method: 'GET',
			headers: token ? { Authorization: `Bearer ${token}` } : {}
		});
		if (response.status === 204 || response.status === 404) return null;
		if (!response.ok) return null;
		const blob = await response.blob();
		return URL.createObjectURL(blob);
	} catch {
		return null;
	}
}

export const avatarService = {
	getAvatarApiUrl,
	fetchAvatarBlobUrl
};

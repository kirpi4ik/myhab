import { Utils } from '@/_helpers';
import { authzService } from '@/_services';

/**
 * REST client for MegaD device discovery, init-from-device, and configuration
 * backup/restore. Backed by DeviceController on the server.
 */

function authHeaders() {
	const token = authzService.currentUserValue?.access_token || authzService.currentUserValue?.token;
	const headers = { 'Content-Type': 'application/json' };
	if (token) headers.Authorization = `Bearer ${token}`;
	return headers;
}

async function request(path, options = {}) {
	const url = `${Utils.host()}${path}`;
	const response = await fetch(url, { ...options, headers: { ...authHeaders(), ...(options.headers || {}) } });
	const text = await response.text();
	const data = text ? JSON.parse(text) : null;
	if (!response.ok) {
		const message = (data && (data.error || data.message)) || response.statusText || `HTTP ${response.status}`;
		const error = new Error(message);
		error.status = response.status;
		error.payload = data;
		throw error;
	}
	return data;
}

export const deviceService = {
	/**
	 * UDP-broadcast scan for MegaD controllers on the local network.
	 * @returns {Promise<{devices: Array<{ip: string, bootloaderMode: boolean, mqttId: ?string}>}>}
	 */
	discoverDevices() {
		return request('/api/devices/discover', { method: 'GET' });
	},

	/**
	 * Read full configuration from a live controller and create a Device row.
	 * @param {string} ip
	 * @param {string} password
	 * @returns {Promise<{deviceId: number, deviceCode: string, portCount: number}>}
	 */
	initFromDevice(ip, password) {
		return request('/api/devices/init-from-device', {
			method: 'POST',
			body: JSON.stringify({ ip, password })
		});
	},

	/**
	 * Persist a configuration backup of the controller.
	 * @returns {Promise<{id: number, frmVersion: string, configLines: number}>}
	 */
	backupConfig(deviceId) {
		return request(`/api/devices/${deviceId}/backup`, { method: 'POST' });
	},

	/**
	 * @returns {Promise<{backups: Array<{id, frmVersion, configLines, tsCreated}>}>}
	 */
	listBackups(deviceId) {
		return request(`/api/devices/${deviceId}/backups`, { method: 'GET' });
	},

	/**
	 * Read the current full configuration straight from the controller.
	 */
	readFullConfig(deviceId) {
		return request(`/api/devices/${deviceId}/config`, { method: 'GET' });
	},

	/**
	 * Push a stored backup down to the physical controller (with restart).
	 */
	pushToController(deviceId, backupId) {
		return request(`/api/devices/${deviceId}/push-to-controller/${backupId}`, { method: 'POST' });
	},

	/**
	 * Sync the database Device/DevicePort model from a backup.
	 */
	syncFromBackup(deviceId, backupId) {
		return request(`/api/devices/${deviceId}/sync-from-backup/${backupId}`, { method: 'POST' });
	}
};

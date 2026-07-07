import { defineStore } from 'pinia';
import { Utils } from '@/_helpers';
import { Client } from '@stomp/stompjs';
import { authzService } from '@/_services';
import SockJS from 'sockjs-client';

// Reconnect if the tab was hidden longer than this — defeats the "zombie"
// socket that mobile browsers leave half-open after the device sleeps, where
// StompJS still reports connected but no frames flow.
const RESUME_RECONNECT_THRESHOLD_MS = 10000;

export const useWebSocketStore = defineStore('websocket', {
	state: () => ({
		message: null,
		connection: 'OFFLINE',
		wsStompClient: null,
		// Internal reconnection bookkeeping (not part of the public surface).
		resumeHandlersInstalled: false,
		hiddenSince: null,
	}),

	getters: {
		ws: (state) => ({
			message: state.message,
			connection: state.connection,
		}),
	},

	actions: {
		setMessage(newValue) {
			this.message = newValue;
		},

		setConnection(newValue) {
			this.connection = newValue;
		},

		connect() {
			if (!authzService.currentUserValue) {
				// User not authenticated, skipping WebSocket connection
				return;
			}

			// Idempotent: tear down any previous client before creating a new one
			// so we never leak competing reconnect loops / duplicate sockets.
			if (this.wsStompClient) {
				try {
					this.wsStompClient.deactivate();
				} catch (e) {
					/* already gone */
				}
				this.wsStompClient = null;
			}

			const store = this;
			// Built fresh on every (re)connect attempt so reconnects always use the
			// latest access_token (see the JWT refresh flow in authentication.service).
			const buildUrl = () =>
				Utils.host() + '/stomp?access_token=' + (authzService.currentUserValue?.access_token ?? '');

			const message_callback = (message) => {
				if (message.headers['content-type'] === 'application/octet-stream') {
					store.setMessage(message.binaryBody);
				} else {
					store.setMessage(JSON.parse(message.body));
				}
			};

			this.wsStompClient = new Client({
				debug: function (str) {
					// Debug disabled for production
				},
				// Let StompJS own reconnection with a single client instead of the
				// old setTimeout(connect) that spawned duplicate clients.
				reconnectDelay: 5000,
				heartbeatIncoming: 10000,
				heartbeatOutgoing: 10000,
				webSocketFactory: () => new SockJS(buildUrl()),
				onConnect: () => {
					store.setConnection('ONLINE');
					store.wsStompClient.subscribe('/topic/events', message_callback, {});
				},
				onWebSocketClose: () => store.setConnection('OFFLINE'),
				onStompError: () => store.setConnection('OFFLINE'),
				onWebSocketError: () => store.setConnection('OFFLINE'),
			});
			this.wsStompClient.activate();
			this.installResumeHandlers();
		},

		/**
		 * Register visibility / online / focus handlers exactly once so the socket
		 * self-heals when the device wakes or the network returns. Without this the
		 * page can sit on a dead connection until a manual reload.
		 */
		installResumeHandlers() {
			if (this.resumeHandlersInstalled) return;
			this.resumeHandlersInstalled = true;
			const store = this;

			document.addEventListener('visibilitychange', () => {
				if (document.visibilityState === 'hidden') {
					store.hiddenSince = Date.now();
					return;
				}
				const hiddenMs = store.hiddenSince ? Date.now() - store.hiddenSince : 0;
				store.hiddenSince = null;
				if (hiddenMs > RESUME_RECONNECT_THRESHOLD_MS || store.connection !== 'ONLINE') {
					store.forceReconnect();
				}
			});
			window.addEventListener('online', () => store.ensureConnected());
			window.addEventListener('focus', () => store.ensureConnected());
		},

		/** Reconnect only if the socket isn't currently up. */
		ensureConnected() {
			if (!authzService.currentUserValue) return;
			if (!this.wsStompClient || !this.wsStompClient.connected) {
				this.forceReconnect();
			}
		},

		/** Tear down the current client and establish a fresh connection. */
		forceReconnect() {
			if (!authzService.currentUserValue) return;
			this.setConnection('OFFLINE');
			this.connect();
		},

		/**
		 * Subscribe to an arbitrary STOMP destination, handing each frame's parsed
		 * JSON body straight to `callback`. Unlike the shared `/topic/events` slot
		 * this is lossless (no single overwritten ref), so it suits high-volume
		 * feeds like the raw MQTT stream. Returns the StompJS subscription (call
		 * `.unsubscribe()` to stop), or null if not connected yet — callers should
		 * (re)subscribe once `connection` becomes 'ONLINE'.
		 */
		subscribe(destination, callback) {
			if (this.wsStompClient && this.connection === 'ONLINE') {
				return this.wsStompClient.subscribe(destination, (m) => {
					try {
						callback(JSON.parse(m.body));
					} catch (e) {
						/* ignore malformed frame */
					}
				});
			}
			return null;
		},
	},
});


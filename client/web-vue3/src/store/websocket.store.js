import { defineStore } from 'pinia';
import { Utils } from '@/_helpers';
import { Client } from '@stomp/stompjs';
import { authzService } from '@/_services';
import SockJS from 'sockjs-client';

export const useWebSocketStore = defineStore('websocket', {
	state: () => ({
		message: null,
		connection: 'OFFLINE',
		wsStompClient: null,
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

		connect(handler) {
			if (authzService.currentUserValue) {
				const wsUri = Utils.host() + '/stomp?access_token=' + authzService.currentUserValue.access_token;

				const message_callback = (message) => {
					if (message.headers['content-type'] === 'application/octet-stream') {
						this.setMessage(message.binaryBody);
					} else {
						this.setMessage(JSON.parse(message.body));
					}
				};

				this.wsStompClient = new Client({
					brokerURL: wsUri,
					debug: function (str) {
						// Debug disabled for production
					},
					heartbeatIncoming: 4000,
					heartbeatOutgoing: 4000,
					webSocketFactory: function () {
						return new SockJS(wsUri);
					},
					onConnect: () => {
						this.setConnection('ONLINE');
						this.wsStompClient.subscribe('/topic/events', message_callback, {});
						if (handler != null) {
							this.wsStompClient.subscribe('/topic/events', handler, {});
						}
					},
					onStompError: (error) => this.stompFailureCallback(error),
					onWebSocketError: (error) => this.stompFailureCallback(error),
				});
				this.wsStompClient.activate();
			} else {
				// User not authenticated, skipping WebSocket connection
			}
		},

	stompFailureCallback(error) {
		this.setConnection('OFFLINE');
		// WebSocket error - will reconnect in 5 seconds
		setTimeout(() => this.connect(), 5000);
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


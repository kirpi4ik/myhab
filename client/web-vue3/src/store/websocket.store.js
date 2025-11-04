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
	},
});


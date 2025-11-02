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
			if (process.env.DEV) {
				console.log('WS connect action called, currentUser:', authzService.currentUserValue);
			}
			if (authzService.currentUserValue) {
				const wsUri = Utils.host() + '/stomp?access_token=' + authzService.currentUserValue.access_token;
				if (process.env.DEV) {
					console.log('STOMP: Attempting connection to:', wsUri);
				}

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
						if (process.env.DEV) {
							console.log(str);
						}
					},
					heartbeatIncoming: 4000,
					heartbeatOutgoing: 4000,
					webSocketFactory: function () {
						return new SockJS(wsUri);
					},
					onConnect: () => {
						if (process.env.DEV) {
							console.log('CONNECTED');
						}
						this.setConnection('ONLINE');
						this.wsStompClient.subscribe('/topic/events', message_callback, {});
						if (handler != null) {
							this.wsStompClient.subscribe('/topic/events', handler, {});
						}
					},
					onStompError: (error) => this.stompFailureCallback(error),
					onWebSocketError: (error) => this.stompFailureCallback(error),
				});
				if (process.env.DEV) {
					console.log('Connecting...');
				}
				this.wsStompClient.activate();
			} else {
				if (process.env.DEV) {
					console.log('STOMP: User not authenticated, skipping connection');
				}
			}
		},

		stompFailureCallback(error) {
			this.setConnection('OFFLINE');
			if (process.env.DEV) {
				console.log('STOMP error: ' + error);
			}
			setTimeout(() => this.connect(), 5000);
			if (process.env.DEV) {
				console.log('STOMP: Reconnecting in 5 seconds');
			}
		},
	},
});


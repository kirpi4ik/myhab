import { Utils } from '@/_helpers';
import { Client } from '@stomp/stompjs';
import { authzService } from '@/_services';
import SockJS from 'sockjs-client';

const state = {
	message: null,
	connection: 'OFFLINE',
};

const getters = {
	ws: state => {
		return state;
	},
};

const mutations = {
	wsMessage(state, newValue) {
		state.message = newValue;
	},
	wsConnection(state, newValue) {
		state.connection = newValue;
	},
};

let wsStompClient = null;
const actions = {
	connect(context, handler) {
		if (authzService.currentUserValue) {
			const wsUri = Utils.host() + '/stomp?access_token=' + authzService.currentUserValue.access_token;
			console.log('STOMP: Attempting connection');

			let message_callback = function (message) {
				if (message.headers['content-type'] === 'application/octet-stream') {
					this.commit('wsMessage', message.binaryBody);
				} else {
					this.commit('wsMessage', JSON.parse(message.body));
				}
			}.bind(this);

			this.wsStompClient = new Client({
				brokerURL: wsUri,
				debug: function (str) {
					console.log(str);
				},
				heartbeatIncoming: 4000,
				heartbeatOutgoing: 4000,
				webSocketFactory: function () {
					return new SockJS(wsUri);
				},
				onConnect: () => {
					console.log('CONNECTED');
					this.commit('wsConnection', 'ONLINE');
					this.wsStompClient.subscribe('/topic/events', message_callback, {});
					if (handler != null) {
						this.wsStompClient.subscribe('/topic/events', handler, {});
					}
				},
				onStompError: this.stompFailureCallback,
				onWebSocketError: this.stompFailureCallback,
			});
			console.log('Connecting...');
			this.wsStompClient.activate();
		}
	},
	stompFailureCallback(error) {
		this.commit('wsConnection', 'OFFLINE');
		console.log('STOMP error: ' + error);
		setTimeout(this.connect, 5000);
		console.log('STOMP: Reconecting in 5 seconds');
	},
};

export default {
	namespaced: false,
	state,
	mutations,
	actions,
	getters,
};

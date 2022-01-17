import {Stomp, Client, ActivationState} from "@stomp/stompjs";
import {Utils} from '@/_helpers';
import SockJS from "sockjs-client";
import {authenticationService} from '@/_services';
import Vue from 'vue'
import Vuex from "vuex";


Vue.use(Vuex);

let stompClient = null;
const state = {
    stomp: {
        message: null,
        connection: 'OFFLINE'
    }
};
const mutations = {
    init(state) {

    },
    stompValue(state, newValue) {
        state.stomp.message = newValue;

    },
    stompConnection(state, newValue) {
        state.stomp.connection = newValue;

    }
};
const actions = {
    connect(context, handler) {
        const wsUri = Utils.host() + '/stomp?access_token=' + authenticationService.currentUserValue.access_token;
        console.log('STOMP: Attempting connection');

        let message_callback = function (message) {
            if (message.headers['content-type'] === 'application/octet-stream') {
                this.commit("stompValue", message.binaryBody);
            } else {
                this.commit("stompValue", JSON.parse(message.body));
            }
        }.bind(this);

        this.stompClient = new Client({
            brokerURL: wsUri,
            debug: function (str) {
                // console.log(str);
            },
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            webSocketFactory: function () {
                return new SockJS(wsUri);
            },
            onConnect: () => {
                console.log("CONNECTED");
                this.commit("stompConnection", 'ONLINE');
                this.stompClient.subscribe("/topic/events", message_callback, {});
                if (handler != null) {
                    this.stompClient.subscribe("/topic/events", handler, {});
                }
            },
            onStompError: this.stompFailureCallback,
            onWebSocketError: this.stompFailureCallback
        });
        console.log('Connecting...');
        this.stompClient.activate();
    },
    stompFailureCallback(error) {
        this.commit("stompConnection", 'OFFLINE');
        console.log('STOMP error: ' + error);
        setTimeout(this.connect, 5000);
        console.log('STOMP: Reconecting in 5 seconds');
    }
};
const store = new Vuex.Store({
        state,
        mutations,
        actions
    }
);
export const vuex = {
    store
};
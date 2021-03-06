import Vue from 'vue'
import Vuelidate from 'vuelidate';

import moment from 'moment';
import VueMoment from 'vue-moment';

import App from './App'
import {router} from './_helpers';

import CoreuiVue from '@coreui/vue'
import {iconsSet as icons} from './assets/icons/icons.js'

import {vuex} from '@/_helpers';
import {library} from '@fortawesome/fontawesome-svg-core'
import {fas} from '@fortawesome/free-solid-svg-icons'
import {fab} from '@fortawesome/free-brands-svg-icons'
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome'
import ToggleButton from 'vue-js-toggle-button'
import apolloProvider from './graphql';

import {Datetime} from 'vue-datetime'
// You need a specific loader for CSS files
import 'vue-datetime/dist/vue-datetime.css'

import i18n from './i18n'

Vue.use(Datetime);
Vue.component('datetime', Datetime);

library.add(fas, fab);
Vue.component('font-awesome-icon', FontAwesomeIcon);

Vue.config.performance = true;
Vue.use(CoreuiVue);
Vue.use(Vuelidate);
Vue.use(ToggleButton);

Vue.use(VueMoment, {moment});

const store = vuex.store;
const stompClient = vuex.stompClient;
new Vue({
    store,
    stompClient,
    el: '#app',
    router,
    icons,
    template: '<App/>',

    components: {
        App
    },

    i18n,
    apolloProvider
});
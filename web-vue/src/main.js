import Vue from 'vue'
import Vuex from 'vuex'
import Vuelidate from 'vuelidate';

import App from './App'
import {router} from './_helpers';

import CoreuiVue from '@coreui/vue'
import {iconsSet as icons} from './assets/icons/icons.js'

import {library} from '@fortawesome/fontawesome-svg-core'
import {fas} from '@fortawesome/free-solid-svg-icons'
import {fab} from '@fortawesome/free-brands-svg-icons'
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome'
import ToggleButton from 'vue-js-toggle-button'
import apolloProvider from './graphql';


library.add(fas, fab);
Vue.component('font-awesome-icon', FontAwesomeIcon);

Vue.config.performance = true;
Vue.use(CoreuiVue);
Vue.use(Vuelidate);
Vue.use(ToggleButton);
Vue.use(Vuex);

const store = new Vuex.Store({
        state: {
            config: {
                grafanaUrl: "http://localhost:3000"
            }
        },
        mutations: {
            init(state) {

            }
        }
    }
);
new Vue({
    store,
    el: '#app',
    router,
    icons,
    template: '<App/>',
    components: {
        App
    },
    apolloProvider
});

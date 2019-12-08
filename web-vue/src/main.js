import Vue from 'vue'
import Vuelidate from 'vuelidate';

import App from './App'
import {router} from './_helpers';

import CoreuiVue from '@coreui/vue'
import {iconsSet as icons} from './assets/icons/icons.js'

Vue.config.performance = true
Vue.use(CoreuiVue)
Vue.use(Vuelidate);

import apolloProvider from './graphql';

new Vue({
    el: '#app',
    router,
    icons,
    template: '<App/>',
    components: {
        App
    },
    apolloProvider
})

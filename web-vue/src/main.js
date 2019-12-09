import Vue from 'vue'
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


library.add(fas, fab)
Vue.component('font-awesome-icon', FontAwesomeIcon)

Vue.config.performance = true
Vue.use(CoreuiVue)
Vue.use(Vuelidate);
Vue.use(ToggleButton)


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

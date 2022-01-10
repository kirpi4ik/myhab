<template>
    <router-view></router-view>
</template>

<script>
    import {authenticationService} from '@/_services';
    import {router, Role} from '@/_helpers';
    import Vuex from "vuex";


    export default {
        name: 'App',
        data() {
            return {
                currentUser: null
            };
        },
        computed: {
            isAdmin() {
                return this.currentUser && this.currentUser.role === Role.Admin;
            }
        },
        created() {
            authenticationService.currentUser.subscribe(x => this.currentUser = x);
            this.connect();
        },
        methods:{
            ...Vuex.mapActions(["connect"]),
        },
        watch: {
            $route: {
                immediate: true,
                handler(to, from) {
                    document.title = to.meta.title || process.env.VUE_APP_CONF_TITLE + process.env.VUE_APP_CONF_VERSION;
                }
            },
        }
    };
</script>

<style lang="scss">
    // Import Main styles for this application
    @import 'assets/scss/style';
</style>

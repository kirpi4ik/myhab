<template>
    <router-view></router-view>
</template>

<script>
    import {authenticationService} from '@/_services';
    import {router, Role} from '@/_helpers';

    export default {
        name: 'App',
        data() {
            return {
                currentUser: null,
                title: process.env.VUE_APP_TITLE
            };
        },
        computed: {
            isAdmin() {
                return this.currentUser && this.currentUser.role === Role.Admin;
            }
        },
        created() {
            authenticationService.currentUser.subscribe(x => this.currentUser = x);
        }
    };
</script>

<style lang="scss">
    // Import Main styles for this application
    @import 'assets/scss/style';
</style>

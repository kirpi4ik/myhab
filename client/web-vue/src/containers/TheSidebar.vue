<template>
    <CSidebar
            fixed
            :minimize="minimize"
            :show.sync="show"

    >
        <CSidebarBrand>
            <svg viewBox="0 0 800 800" width="50">
                <use xlink:href="/img/logo.svg#logox"/>
            </svg>
        </CSidebarBrand>
        <CRenderFunction flat :content-to-render="navItems"/>
        <CSidebarMinimizer
                class="d-md-down-none"
                @click.native="minimize = !minimize"
        />
    </CSidebar>
</template>

<script>
    import {NAV} from './_nav'
    import {authenticationService} from '@/_services';

    export default {
        name: 'TheSidebar',
        data() {
            return {
                minimize: true,
                navItems: [],
                show: 'responsive'
            }
        },
        mounted() {
            const currentUser = authenticationService.currentUserValue;
            let filterNav = function (navItem) {
                let hasRole = true;
                if (navItem.roles != null) {
                    hasRole = currentUser.permissions.filter(function (userRole) {
                        return navItem.roles.includes(userRole);
                    }).length > 0;

                }
                if (hasRole && navItem._children) {
                    navItem._children = navItem._children.filter(filterNav)
                }
                return hasRole;
            };
            this.navItems = NAV.filter(filterNav);
            this.$root.$on('toggle-sidebar', () => {
                const sidebarOpened = this.show === true || this.show === 'responsive';
                this.show = sidebarOpened ? false : 'responsive'
            });
            this.$root.$on('toggle-sidebar-mobile', () => {
                const sidebarClosed = this.show === 'responsive' || this.show === false;
                this.show = sidebarClosed ? true : 'responsive'
            })
        }
    }
</script>

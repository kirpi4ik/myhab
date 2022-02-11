<template>
    <CHeader fixed with-subheader light>
        <CToggler in-header class="ml-3 d-lg-none" v-c-emit-root-event:toggle-sidebar-mobile/>
        <CToggler in-header class="ml-3 d-md-down-none" v-c-emit-root-event:toggle-sidebar/>
        <CHeaderBrand
                class="mx-auto d-lg-none"
                src="img/brand/coreui-base.svg"
                width="97"
                height="46"
                alt="myHAB Logo"
        />
        <CHeaderNav class="d-md-down-none mr-auto">
            <CHeaderNavItem class="px-3">
                <CHeaderNavLink to="/pages/wui">
                    <CIcon name="cil-mobile" style="width: 30px; height: 30px"/>
                </CHeaderNavLink>
            </CHeaderNavItem>
            <CHeaderNavItem class="px-3">
                <CHeaderNavLink to="/nx">
                    <b>NX</b>
                </CHeaderNavLink>
            </CHeaderNavItem>
        </CHeaderNav>
        <CHeaderNav class="mr-4">
            <ClockComponent/>
            <font-awesome-icon :icon="['fas', 'wifi']" size="1x" style="color: green" v-if="stompConnection == 'ONLINE'" title="Server connected"/>
            <font-awesome-icon :icon="['fas', 'exclamation-triangle']" size="1x" style="color: red" v-if="stompConnection == 'OFFLINE'" title="Async connection offline"/>
            <TheHeaderDropdownNotif/>
            <TheHeaderDropdownTaskList v-if="hasRole(['ROLE_ADMIN'])"/>
            <TheHeaderDropdownMsgInbox/>
            <TheHeaderDropdownAccnt/>
        </CHeaderNav>
        <CSubheader class="px-3">
            <BreadcrumbRouteNav class="border-0" ref="mainBreadcrumb"/>
        </CSubheader>
    </CHeader>
</template>

<script>
    import TheHeaderDropdownAccnt from './TheHeaderDropdownAccnt'
    import TheHeaderDropdownMsgInbox from './TheHeaderDropdownMsgInbox'
    import TheHeaderDropdownNotif from './TheHeaderDropdownNotif'
    import TheHeaderDropdownTaskList from './TheHeaderDropdownTaskList'
    import BreadcrumbRouteNav from './BreadcrumbRouteNav'
    import ClockComponent from './ClockComponent'
    import {authenticationService} from '@/_services';

    export default {
        name: 'TheHeader',
        components: {
            TheHeaderDropdownAccnt,
            TheHeaderDropdownNotif,
            TheHeaderDropdownMsgInbox,
            TheHeaderDropdownTaskList,
            BreadcrumbRouteNav,
            ClockComponent
        },
        computed: {
            stompConnection() {
                return this.$store.state.stomp.connection
            }
        },
        methods: {
            hasRole: function (roles) {
                const currentUser = authenticationService.currentUserValue;
                return currentUser.permissions.filter(function (userRole) {
                    return roles.includes(userRole);
                }).length > 0;
            }
        }
    }
</script>

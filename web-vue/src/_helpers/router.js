import Vue from 'vue';
import Router from 'vue-router';

import {authenticationService} from '@/_services';
import {Role} from '@/_helpers';

// Containers
const TheContainer = () => import('@/containers/TheContainer');

// Views
const Dashboard = () => import('@/views/dashboard/Dashboard');

const Zones = () => import('@/views/zones/ZoneCombinedView');
// Users
const Users = () => import('@/views/users/Users');
const User = () => import('@/views/users/User');
const UserEdit = () => import('@/views/users/UserEdit');
const UserNew = () => import('@/views/users/UserNew');

const Devices = () => import('@/views/devices/Devices');
const Device = () => import('@/views/devices/Device');
const DeviceEdit = () => import('@/views/devices/DeviceEdit');
const DeviceNew = () => import('@/views/devices/DeviceNew');

const Peripherals = () => import('@/views/peripherals/Peripherals');
const Peripheral = () => import('@/views/peripherals/Peripheral');
const PeripheralEdit = () => import('@/views/peripherals/PeripheralEdit');
const PeripheralNew = () => import('@/views/peripherals/PeripheralNew');

const Cables = () => import('@/views/cables/Cables');
const Cable = () => import('@/views/cables/Cable');
const CableEdit = () => import('@/views/cables/CableEdit');
const CableNew = () => import('@/views/cables/CableNew');

const RouteContainer = () => import('@/containers/RouteContainer');

// Views - Pages
const Page403 = () => import('@/views/pages/Page403');
const Page404 = () => import('@/views/pages/Page404');
const Page500 = () => import('@/views/pages/Page500');
const Login = () => import('@/views/pages/Login');
const Register = () => import('@/views/pages/Register');


Vue.use(Router);

export const router = new Router({
    mode: 'hash', // https://router.vuejs.org/api/#mode
    linkActiveClass: 'active',
    scrollBehavior: () => ({y: 0}),
    routes: configRoutes()
});

function configRoutes() {
    return [
        {
            path: '/',
            redirect: '/dashboard',
            name: 'Home',
            component: TheContainer,
            children: [
                {
                    path: 'dashboard',
                    name: 'Dashboard',
                    component: Dashboard,
                    meta: {authorize: [Role.Admin, Role.User]}
                },
                {
                    path: 'zones',
                    name: 'Zones',
                    component: Zones,
                    meta: {authorize: [Role.Admin, Role.User]}
                },
                {
                    path: 'users',
                    meta: {
                        label: 'Users',
                        reload: true,
                    },
                    component: RouteContainer,
                    children: [
                        {
                            path: '',
                            component: Users,
                            meta: {
                                reload: true,
                            }
                        },
                        {
                            path: '/users/:id/profile',
                            name: "Details",
                            component: User
                        },
                        {
                            path: '/users/:id/edit',
                            name: "Edit",
                            component: UserEdit
                        },
                        {
                            path: '/users/create',
                            name: "New user",
                            component: UserNew
                        }
                    ]
                },
                {
                    path: 'devices',
                    meta: {
                        label: 'Devices',
                        reload: true,
                    },
                    component: RouteContainer,
                    children: [
                        {
                            path: '',
                            component: Devices,
                            meta: {
                                reload: true,
                            }
                        },
                        {
                            path: '/devices/:id/view',
                            name: "Details",
                            component: Device
                        },
                        {
                            path: '/devices/:id/edit',
                            name: "Edit",
                            component: DeviceEdit
                        },
                        {
                            path: '/devices/create',
                            name: "New device",
                            component: DeviceNew
                        }
                    ]
                },
                {
                    path: 'peripherals',
                    meta: {
                        label: 'Peripherals',
                        reload: true,
                    },
                    component: RouteContainer,
                    children: [
                        {
                            path: '',
                            component: Peripherals,
                            meta: {
                                reload: true,
                            }
                        },
                        {
                            path: '/peripherals/:id/view',
                            name: "Peripheral",
                            component: Peripheral
                        },
                        {
                            path: '/peripherals/:id/edit',
                            name: "Edit",
                            component: PeripheralEdit
                        },
                        {
                            path: '/peripherals/create',
                            name: "New peripheral",
                            component: PeripheralNew
                        }
                    ]
                },
                {
                    path: 'cables',
                    meta: {
                        label: 'Cables',
                        reload: true,
                    },
                    component: RouteContainer,
                    children: [
                        {
                            path: '',
                            component: Cables,
                            meta: {
                                reload: true,
                            }
                        },
                        {
                            path: '/cables/:id/view',
                            name: "Cables",
                            component: Cable
                        },
                        {
                            path: '/cables/:id/edit',
                            name: "Edit",
                            component: CableEdit
                        },
                        {
                            path: '/cables/create',
                            name: "New cable",
                            component: CableNew
                        }
                    ]
                }
                // otherwise redirect to home
                // {path: '*', redirect: '/'}
            ]
        },
        {
            path: '/pages',
            redirect: '/pages/404',
            name: 'Pages',
            component: {
                render(c) {
                    return c('router-view')
                }
            },
            children: [
                {
                    path: '404',
                    name: 'Page404',
                    component: Page404
                },
                {
                    path: '403',
                    name: 'Page403',
                    component: Page403
                },
                {
                    path: '500',
                    name: 'Page500',
                    component: Page500
                },
                {
                    path: 'login',
                    name: 'Login',
                    component: Login
                },
                {
                    path: 'register',
                    name: 'Register',
                    component: Register
                }
            ]
        }
    ]
}

router.beforeEach((to, from, next) => {
    // redirect to login page if not logged in and trying to access a restricted page
    const {authorize} = to.meta;
    const currentUser = authenticationService.currentUserValue;

    if (authorize) {
        if (!currentUser) {
            // not logged in so redirect to login page with the return url
            return next({path: '/pages/login', query: {returnUrl: to.path}});
        }

        // check if route is restricted by role
        const hasRole = currentUser.permissions.filter(function (userRole) {
            return authorize.includes(userRole);
        }).length > 0
        if (authorize.length && !hasRole) {
            // role not authorised so redirect to home page
            return next({path: '/pages/403'});
        }
    }

    next();
})


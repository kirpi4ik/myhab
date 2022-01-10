import Vue from 'vue';
import Router from 'vue-router';

import {authenticationService} from '@/_services';
import {Role} from '@/_helpers';

import i18n from './../i18n'

// Containers
const TheContainer = () => import('@/containers/TheContainer');

// Views
const Dashboard = () => import('@/views/dashboard/Dashboard');

const ConfigList = () => import('@/views/configuration/ConfigList');

const Zones = () => import('@/views/zones/ZoneCombinedView');
// Users
const UserList = () => import('@/views/users/UserList');
const UserView = () => import('@/views/users/UserView');
const UserEdit = () => import('@/views/users/UserEdit');
const UserNew = () => import('@/views/users/UserNew');

const DeviceList = () => import('@/views/devices/DeviceList');
const DeviceView = () => import('@/views/devices/DeviceView');
const DeviceEdit = () => import('@/views/devices/DeviceEdit');
const DeviceNew = () => import('@/views/devices/DeviceNew');

const PeripheralList = () => import('@/views/peripherals/PeripheralList');
const PeripheralView = () => import('@/views/peripherals/PeripheralView');
const PeripheralEdit = () => import('@/views/peripherals/PeripheralEdit');
const PeripheralNew = () => import('@/views/peripherals/PeripheralNew');

const PortView = () => import('@/views/ports/PortView');
const PortEditor = () => import('@/views/ports/PortEditor');

const CableList = () => import('@/views/cables/CableList');
const CableView = () => import('@/views/cables/CableView');
const CableEdit = () => import('@/views/cables/CableEdit');
const CableNew = () => import('@/views/cables/CableEdit');

const RouteContainer = () => import('@/containers/RouteContainer');

// Views - Pages
const Page403 = () => import('@/views/pages/Page403');
const Page404 = () => import('@/views/pages/Page404');
const Page500 = () => import('@/views/pages/Page500');
const Login = () => import('@/views/pages/Login');
const Wui = () => import('@/views/pages/wui');
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
            name: i18n.t('breadcrumb.home'),
            component: TheContainer,
            children: [
                {
                    path: 'dashboard',
                    name: i18n.t('breadcrumb.dashboard'),
                    component: Dashboard,
                    meta: {authorize: [Role.Admin, Role.User]}
                },
                {
                    path: 'zones/:zoneId/',
                    name: i18n.t('breadcrumb.zones'),
                    component: Zones,
                    meta: {authorize: [Role.Admin, Role.User]}
                },
                {
                    path: 'users',
                    meta: {
                        label: i18n.t('breadcrumb.users'),
                        reload: true
                    },
                    component: RouteContainer,
                    children: [
                        {
                            path: '',
                            component: UserList,
                            meta: {
                                reload: true,
                            }
                        },
                        {
                            path: '/users/:idPrimary/profile',
                            name: i18n.t('breadcrumb.user.details'),
                            component: UserView
                        },
                        {
                            path: '/users/:idPrimary/edit',
                            name: i18n.t('breadcrumb.user.edit'),
                            component: UserEdit
                        },
                        {
                            path: '/users/create',
                            name: i18n.t('breadcrumb.user.create'),
                            component: UserNew
                        }
                    ]
                },
                {
                    path: 'devices',
                    meta: {
                        label: i18n.t('breadcrumb.devices'),
                        reload: true,
                    },
                    component: RouteContainer,
                    children: [
                        {
                            path: '',
                            component: DeviceList,
                            meta: {
                                reload: true,
                            }
                        },
                        {
                            path: 'create',
                            name: i18n.t('breadcrumb.device.create'),
                            component: DeviceNew
                        },
                        {
                            path: ':idPrimary',
                            component: RouteContainer,
                            children: [
                                {
                                    path: 'ports',
                                    meta: {
                                        label: i18n.t('breadcrumb.ports.list'),
                                        reload: true,
                                    },
                                    component: RouteContainer,
                                    children: [
                                        {
                                            path: 'create',
                                            meta: {
                                                label: i18n.t('breadcrumb.ports.create'),
                                                reload: true,
                                                uiMode: 'CREATE'
                                            },
                                            component: PortEditor
                                        },
                                        {
                                            path: ':id',
                                            component: RouteContainer,
                                            meta: {
                                                reload: true,
                                            },
                                            children: [
                                                {
                                                    path: 'view',
                                                    name: i18n.t('breadcrumb.ports.details'),
                                                    component: PortView,
                                                    meta: {
                                                        uiMode: 'VIEW'
                                                    }
                                                },
                                                {
                                                    path: 'edit',
                                                    name: i18n.t('breadcrumb.ports.edit'),
                                                    component: PortEditor,
                                                    meta: {
                                                        uiMode: 'EDIT'
                                                    }
                                                },
                                                {
                                                    path: 'configurations',
                                                    name: i18n.t('breadcrumb.configurations'),
                                                    component: ConfigList,
                                                    meta: {
                                                        entityType: 'PORT'
                                                    }
                                                }]
                                        }
                                    ]
                                },

                                {
                                    path: 'view',
                                    name: i18n.t('breadcrumb.device.details'),
                                    component: DeviceView,
                                    meta: {
                                        uiMode: 'VIEW'
                                    }
                                },
                                {
                                    path: 'configurations',
                                    name: i18n.t('breadcrumb.configurations'),
                                    component: ConfigList,
                                    meta: {
                                        entityType: 'DEVICE'
                                    }
                                },
                                {
                                    path: 'edit',
                                    name: i18n.t('breadcrumb.device.edit'),
                                    component: DeviceEdit,
                                    meta: {
                                        uiMode: 'EDIT'
                                    }
                                }
                            ]

                        }

                    ]
                },
                {
                    path: 'peripherals',
                    meta: {
                        label: i18n.t('breadcrumb.peripherals'),
                        reload: true,
                    },
                    component: RouteContainer,
                    children: [
                        {
                            path: '',
                            component: PeripheralList,
                            meta: {
                                reload: true,
                            }
                        },
                        {
                            path: 'create',
                            component: PeripheralNew,
                            meta: {
                                reload: true,
                            }
                        },
                        {
                            path: ':idPrimary',
                            component: RouteContainer,
                            children: [
                                {
                                    path: 'view',
                                    name: i18n.t('breadcrumb.peripheral.details'),
                                    component: PeripheralView,
                                    meta: {
                                        uiMode: 'VIEW'
                                    }
                                },
                                {
                                    path: 'configurations',
                                    name: i18n.t('breadcrumb.configurations'),
                                    component: ConfigList,
                                    meta: {
                                        entityType: 'PERIPHERAL'
                                    }
                                },
                                {
                                    path: 'edit',
                                    name: i18n.t('breadcrumb.peripheral.edit'),
                                    component: PeripheralEdit,
                                    meta: {
                                        uiMode: 'EDIT'
                                    }
                                }

                            ]

                        }

                    ]
                },

                {
                    path: 'cables',
                    meta: {
                        label: i18n.t('breadcrumb.cables'),
                        reload: true,
                    },
                    component: RouteContainer,
                    children: [
                        {
                            path: '',
                            component: CableList,
                            meta: {
                                reload: true,
                            }
                        },
                        {
                            path: 'create',
                            name: i18n.t('breadcrumb.cable.create'),
                            component: CableNew,
                            meta: {
                                uiMode: 'CREATE'
                            }
                        },
                        {
                            path: ':idPrimary',
                            component: RouteContainer,
                            children: [
                                {
                                    path: 'view',
                                    name: i18n.t('breadcrumb.cable.details'),
                                    component: CableView,
                                    meta: {
                                        uiMode: 'VIEW'
                                    }
                                },
                                {
                                    path: 'configurations',
                                    name: i18n.t('breadcrumb.configurations'),
                                    component: ConfigList,
                                    meta: {
                                        entityType: 'PERIPHERAL'
                                    }
                                },
                                {
                                    path: 'edit',
                                    name: i18n.t('breadcrumb.cable.edit'),
                                    component: CableEdit,
                                    meta: {
                                        uiMode: 'EDIT'
                                    }
                                }

                            ]

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
                },
                {
                    path: 'wui',
                    name: 'WUI',
                    component: Wui
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


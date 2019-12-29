import Vue from 'vue';
import Router from 'vue-router';

import {authenticationService} from '@/_services';
import {Role} from '@/_helpers';

// Containers
const TheContainer = () => import('@/containers/TheContainer')

// Views
const Dashboard = () => import('@/views/dashboard/Dashboard')

const Zones = () => import('@/views/zones/ZoneCombinedView')
// Users
const Users = () => import('@/views/users/Users')
const User = () => import('@/views/users/User')
const UserEdit = () => import('@/views/users/UserEdit')
const RouteContainer = () => import('@/containers/RouteContainer')

// Views - Pages
const Page403 = () => import('@/views/pages/Page403')
const Page404 = () => import('@/views/pages/Page404')
const Page500 = () => import('@/views/pages/Page500')
const Login = () => import('@/views/pages/Login')
const Register = () => import('@/views/pages/Register')


Vue.use(Router)

export const router = new Router({
    mode: 'hash', // https://router.vuejs.org/api/#mode
    linkActiveClass: 'active',
    scrollBehavior: () => ({y: 0}),
    routes: configRoutes()
})

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


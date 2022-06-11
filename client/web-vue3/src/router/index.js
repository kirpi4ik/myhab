import {route} from 'quasar/wrappers';
import {createMemoryHistory, createRouter, createWebHashHistory, createWebHistory} from 'vue-router';
import routes from './routes';
import {authzService} from '@/_services';

/*
 * If not building with SSR mode, you can
 * directly export the Router instantiation;
 *
 * The function below can be async too; either use
 * async/await or return a Promise which resolves
 * with the Router instance.
 */

export default route(function (/* { store, ssrContext } */) {
  const createHistory =
    process.env.MODE === 'ssr'
      ? createMemoryHistory
      : process.env.VUE_ROUTER_MODE === 'history'
        ? createWebHistory
        : createWebHashHistory;

  const router = createRouter({
    scrollBehavior: () => ({left: 0, top: 0}),
    routes,

    // Leave this as is and make changes in quasar.conf.js instead!
    // quasar.conf.js -> build -> vueRouterMode
    // quasar.conf.js -> build -> publicPath
    history: createHistory(process.env.MODE === 'ssr' ? void 0 : process.env.VUE_ROUTER_BASE),
  });
  router.beforeEach((to, from, next) => {
    // redirect to login page if not logged in and trying to access a restricted page
    const {authorize} = to.meta;
    const currentUser = authzService.currentUserValue;

    if (authorize) {
      if (!currentUser) {
        // not logged in so redirect to login page with the return url
        return next({path: '/login', query: {returnUrl: to.path}});
      }

      // check if route is restricted by role
      const hasRole = currentUser.permissions.filter(function (userRole) {
        return authorize.includes(userRole);
      }).length > 0;
      if (authorize.length && !hasRole) {
        // role not authorised so redirect to home page
        return next({path: '/pages/403'});
      }
    }

    next();
  });

  return router;
});

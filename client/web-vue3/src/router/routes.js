import {Role} from '@/_helpers/role';

const adminPrefix = '/admin';
const routes = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        component: () => import('pages/Dashboard.vue'),
      },
      {
        path: '/zones',
        component: () => import('pages/ZoneCombinedView'),
        children: [
          {
            path: '/zones/:zoneId',
            name: 'zoneById',
            component: () => import('pages/ZoneCombinedView'),
            meta: {
              reload: true,
              navigation: {
                type: 'ZONE',
                id: 'zoneId'
              }
            },
          },
        ],
      },
      {
        path: adminPrefix + '/users',
        component: () => import('layouts/CenterLayout'),
        children: [
          {
            path: adminPrefix + '/users/new',
            component: () => import('pages/infra/user/UserNew'),
          },
          {
            path: '',
            component: () => import('pages/infra/user/UserList'),
          },
          {
            path: adminPrefix + '/users/:idPrimary/view',
            component: () => import('pages/infra/user/UserView'),
          },
          {
            path: adminPrefix + '/users/:idPrimary/edit',
            component: () => import('pages/infra/user/UserEdit'),
          },
        ],
      },
      {
        path: adminPrefix + '/cables',
        component: () => import('layouts/CenterLayout'),
        children: [
          {
            path: adminPrefix + '/cables/new',
            component: () => import('pages/infra/cable/CableNew'),
          },
          {
            path: '',
            component: () => import('pages/infra/cable/CableList'),
          },
          {
            path: adminPrefix + '/cables/:idPrimary/view',
            component: () => import('pages/infra/cable/CableView'),
          },
          {
            path: adminPrefix + '/cables/:idPrimary/edit',
            component: () => import('pages/infra/cable/CableEdit'),
          },
        ],
      },
      {
        path: adminPrefix + '/devices',
        component: () => import('layouts/CenterLayout'),
        children: [
          {
            path: '',
            component: () => import('pages/infra/device/DeviceList'),
          },
          {
            path: adminPrefix + '/devices/new',
            component: () => import('pages/infra/device/DeviceNew'),
          },
          {
            path: adminPrefix + '/devices/:idPrimary/view',
            component: () => import('pages/infra/device/DeviceView'),
          },
          {
            path: adminPrefix + '/devices/:idPrimary/edit',
            component: () => import('pages/infra/device/DeviceEdit'),
          },
        ],
      },
      {
        path: adminPrefix + '/peripherals',
        component: () => import('layouts/CenterLayout'),
        children: [
          {
            path: '',
            component: () => import('pages/infra/peripheral/PeripheralList'),
          },
          {
            path: adminPrefix + '/peripherals/new',
            component: () => import('pages/infra/peripheral/PeripheralNew'),
          },
          {
            path: adminPrefix + '/peripherals/:idPrimary/view',
            component: () => import('pages/infra/peripheral/PeripheralView'),
          },
          {
            path: adminPrefix + '/peripherals/:idPrimary/edit',
            component: () => import('pages/infra/peripheral/PeripheralEdit'),
          },
        ],
      },
      {
        path: adminPrefix + '/zones',
        component: () => import('layouts/CenterLayout'),
        children: [
          {
            path: '',
            component: () => import('pages/infra/zone/ZoneList'),
          },
          {
            path: adminPrefix + '/zones/new',
            component: () => import('pages/infra/zone/ZoneNew'),
          },
          {
            path: adminPrefix + '/zones/:idPrimary/view',
            component: () => import('pages/infra/zone/ZoneView'),
          },
          {
            path: adminPrefix + '/zones/:idPrimary/edit',
            component: () => import('pages/infra/zone/ZoneEdit'),
          },
        ],
      },
      {
        path: adminPrefix + '/ports',
        component: () => import('layouts/CenterLayout'),
        children: [
          {
            path: '',
            component: () => import('pages/infra/port/PortList'),
          },
          {
            path: adminPrefix + '/ports/new',
            component: () => import('pages/infra/port/PortNew'),
          },
          {
            path: adminPrefix + '/ports/:idPrimary/view',
            component: () => import('pages/infra/port/PortView'),
          },
          {
            path: adminPrefix + '/ports/:idPrimary/edit',
            component: () => import('pages/infra/port/PortEdit'),
          },
        ],
      },
    ],
    meta: {authorize: [Role.Admin, Role.User]},
  },
  {
    path: '/wui',
    component: () => import('pages/MobileWebLayout'),
    meta: {authorize: [Role.Admin, Role.User]},
  },
  // Always leave this as last one,
  // but you can also remove it
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorPage404.vue'),
  },
  {
    path: '/Maintenance',
    component: () => import('pages/ErrorPageMaintenance.vue'),
  },
  {
    path: '/login',
    component: () => import('pages/LoginPage.vue'),
  },
  {
    path: '/Lock',
    component: () => import('pages/LockScreen.vue'),
  },
  {
    path: '/Lock-2',
    component: () => import('pages/LockScreen-2.vue'),
  },
];

export default routes;

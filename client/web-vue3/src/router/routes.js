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
                id: 'zoneId',
              },
            },
          },
        ],
      },
      {
        path: adminPrefix + '/users',
        component: () => import('layouts/CenterLayout'),
        meta: {
          reload: true,
          name: "Users"
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/user/UserList')
          },
          {
            path: adminPrefix + '/users/new',
            component: () => import('pages/infra/user/UserNew'),
            meta: {
              reload: true,
              name: "New"
            }
          },
          {
            path: adminPrefix + '/users/:idPrimary/view',
            component: () => import('pages/infra/user/UserView'),
            meta: {
              reload: true,
              name: "Details"
            }
          },
          {
            path: adminPrefix + '/users/:idPrimary/edit',
            component: () => import('pages/infra/user/UserEdit'),
            meta: {
              name: "edit"
            }
          },
        ],
      },
      {
        path: adminPrefix + '/cables',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "cables"
        },
        children: [
          {
            path: adminPrefix + '/cables/new',
            component: () => import('pages/infra/cable/CableNew'),
            meta: {
              name: "new"
            }
          },
          {
            path: '',
            component: () => import('pages/infra/cable/CableList'),
          },
          {
            path: adminPrefix + '/cables/:idPrimary/view',
            component: () => import('pages/infra/cable/CableView'),
            meta: {
              name: "details"
            }
          },
          {
            path: adminPrefix + '/cables/:idPrimary/edit',
            component: () => import('pages/infra/cable/CableEdit'),
            meta: {
              name: "edit"
            }
          },
        ],
      },
      {
        path: adminPrefix + '/devices',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "devices"
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/device/DeviceList'),
          },
          {
            path: adminPrefix + '/devices/new',
            component: () => import('pages/infra/device/DeviceNew'),
            meta: {
              name: "new"
            }
          },
          {
            path: adminPrefix + '/devices/:idPrimary/view',
            component: () => import('pages/infra/device/DeviceView'),
            meta: {
              name: "details"
            }
          },
          {
            path: adminPrefix + '/devices/:idPrimary/edit',
            component: () => import('pages/infra/device/DeviceEdit'),
            meta: {
              name: "edit"
            }
          },
        ],
      },
      {
        path: adminPrefix + '/peripherals',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "peripherals"
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/peripheral/PeripheralList'),
          },
          {
            path: adminPrefix + '/peripherals/new',
            component: () => import('pages/infra/peripheral/PeripheralNew'),
            meta: {
              name: "new"
            }
          },
          {
            path: adminPrefix + '/peripherals/:idPrimary/view',
            component: () => import('pages/infra/peripheral/PeripheralView'),
            meta: {
              name: "details"
            }
          },
          {
            path: adminPrefix + '/peripherals/:idPrimary/edit',
            component: () => import('pages/infra/peripheral/PeripheralEdit'),
            meta: {
              name: "edit"
            }
          },
        ],
      },
      {
        path: adminPrefix + '/pcategories',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "peripherals"
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/peripheral/categories/CategoryList'),
          },
          {
            path: adminPrefix + '/pcategories/new',
            component: () => import('pages/infra/peripheral/categories/CategoryNew'),
            meta: {
              name: "new"
            }
          },
          {
            path: adminPrefix + '/pcategories/:idPrimary/view',
            component: () => import('pages/infra/peripheral/categories/CategoryView'),
            meta: {
              name: "details"
            }
          },
          {
            path: adminPrefix + '/pcategories/:idPrimary/edit',
            component: () => import('pages/infra/peripheral/PeripheralEdit'),
            meta: {
              name: "edit"
            }
          },
        ],
      }
      ,
      {
        path: adminPrefix + '/zones',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "zones"
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/zone/ZoneList'),
          },
          {
            path: adminPrefix + '/zones/new',
            component: () => import('pages/infra/zone/ZoneNew'),
            meta: {
              name: "new"
            }
          },
          {
            path: adminPrefix + '/zones/:idPrimary/view',
            component: () => import('pages/infra/zone/ZoneView'),
            meta: {
              name: "details"
            }
          },
          {
            path: adminPrefix + '/zones/:idPrimary/edit',
            component: () => import('pages/infra/zone/ZoneEdit'),
            meta: {
              name: "edit"
            }
          },
        ],
      },
      {
        path: adminPrefix + '/ports',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "ports"
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/port/PortList'),
          },
          {
            path: adminPrefix + '/ports/new',
            component: () => import('pages/infra/port/PortNew'),
            meta: {
              name: "new"
            }
          },
          {
            path: adminPrefix + '/ports/:idPrimary/view',
            component: () => import('pages/infra/port/PortView'),
            meta: {
              name: "details"
            }
          },
          {
            path: adminPrefix + '/ports/:idPrimary/edit',
            component: () => import('pages/infra/port/PortEdit'),
            meta: {
              name: "edit"
            }
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

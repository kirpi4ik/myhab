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
          name: "Users",
          authorize: [Role.Admin]
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
          name: "cables",
          authorize: [Role.Admin]
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
          name: "devices",
          authorize: [Role.Admin]
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
        path: adminPrefix + '/dcategories',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "dcategories",
          authorize: [Role.Admin]
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/device/categories/CategoryList'),
          },
          {
            path: adminPrefix + '/dcategories/new',
            component: () => import('pages/infra/device/categories/CategoryNew'),
            meta: {
              name: "new"
            }
          },
          {
            path: adminPrefix + '/dcategories/:idPrimary/view',
            component: () => import('pages/infra/device/categories/CategoryView'),
            meta: {
              name: "details"
            }
          },
          {
            path: adminPrefix + '/dcategories/:idPrimary/edit',
            component: () => import('pages/infra/device/categories/CategoryEdit'),
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
          name: "peripherals",
          authorize: [Role.Admin]
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
          name: "pcategories",
          authorize: [Role.Admin]
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
            component: () => import('pages/infra/peripheral/categories/CategoryEdit'),
            meta: {
              name: "edit"
            }
          },
        ],
      }
      ,
      {
        path: adminPrefix + '/configurations/:idPrimary',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "configurations",
          authorize: [Role.Admin]
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/config/ConfigurationView'),
          }
        ],
      },
      {
        path: adminPrefix + '/zones',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "zones",
          authorize: [Role.Admin]
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
          name: "ports",
          authorize: [Role.Admin]
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
      {
        path: adminPrefix + '/scenarios',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "scenarios",
          authorize: [Role.Admin]
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/scenario/ScenarioList'),
          },
          {
            path: adminPrefix + '/scenarios/new',
            component: () => import('pages/infra/scenario/ScenarioNew'),
            meta: {
              name: "new"
            }
          },
          {
            path: adminPrefix + '/scenarios/:idPrimary/view',
            component: () => import('pages/infra/scenario/ScenarioView'),
            meta: {
              name: "details"
            }
          },
          {
            path: adminPrefix + '/scenarios/:idPrimary/edit',
            component: () => import('pages/infra/scenario/ScenarioEdit'),
            meta: {
              name: "edit"
            }
          },
        ],
      },
      {
        path: adminPrefix + '/jobs',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "jobs",
          authorize: [Role.Admin]
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/job/JobList'),
          },
          {
            path: adminPrefix + '/jobs/new',
            component: () => import('pages/infra/job/JobNew'),
            meta: {
              name: "new"
            }
          },
          {
            path: adminPrefix + '/jobs/:idPrimary/view',
            component: () => import('pages/infra/job/JobView'),
            meta: {
              name: "details"
            }
          },
          {
            path: adminPrefix + '/jobs/:idPrimary/edit',
            component: () => import('pages/infra/job/JobEdit'),
            meta: {
              name: "edit"
            }
          },
        ],
      },
      {
        path: adminPrefix + '/appconfig',
        component: () => import('layouts/CenterLayout'),
        meta: {
          name: "appconfig",
          authorize: [Role.Admin]
        },
        children: [
          {
            path: '',
            component: () => import('pages/infra/appconfig/AppConfigView'),
          }
        ],
      },
      {
        path: '/solar-reports',
        component: () => import('pages/SolarReports'),
        meta: {
          name: "Solar Reports"
        }
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

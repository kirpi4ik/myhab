const routes = [
	{
		path: '/',
		component: () => import('layouts/MainLayout.vue'),
		children: [{ path: '', component: () => import('pages/Dashboard.vue') }],
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
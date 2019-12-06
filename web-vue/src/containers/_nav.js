export default [
    {
        _name: 'CSidebarNav',
        _children: [
            {
                _name: 'CSidebarNavItem',
                name: 'Dashboard',
                to: '/dashboard',
                icon: 'cil-speedometer',
                badge: {
                    color: 'primary',
                    text: 'NEW'
                }
            },
            {
                _name: 'CSidebarNavTitle',
                _children: ['Control']
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Lumina',
                to: '/theme/colors',
                icon: 'cil-drop'
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Incalzire',
                to: '/theme/typography',
                icon: 'cil-pencil'
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Temperatura',
                to: '/theme/typography',
                icon: 'cil-pencil'
            },
            {
                _name: 'CSidebarNavTitle',
                _children: ['Setari']
            },
            {
                _name: 'CSidebarNavDropdown',
                name: 'Utilizatori',
                route: '/users',
                icon: 'cil-puzzle',
                items: [
                    {
                        name: 'Lista utilizatorilor',
                        to: '/users',
                        icon: 'cil-puzzle'
                    }
                ]
            }
        ]
    }
]
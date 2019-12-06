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
                _children: ['Theme']
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Colors',
                to: '/theme/colors',
                icon: 'cil-drop'
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Typography',
                to: '/theme/typography',
                icon: 'cil-pencil'
            },
            {
                _name: 'CSidebarNavTitle',
                _children: ['Setari']
            },
            {
                _name: 'CSidebarNavDropdown',
                name: 'Cabluri',
                route: '/users',
                icon: 'cil-puzzle',
                items: [
                    {
                        name: 'PDF',
                        to: '/users',
                        icon: 'cil-puzzle'
                    }
                ]
            }
        ]
    }
]
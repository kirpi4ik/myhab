import {Role} from '@/_helpers';

export const NAV = [
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
                _children: ['Control'],
                roles: [Role.Super]
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Lumina',
                to: '/theme/colors',
                icon: 'cil-drop',
                roles: [Role.Super]
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Incalzire',
                to: '/theme/typography',
                icon: 'cil-pencil',
                roles: [Role.Super]
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Temperatura',
                to: '/theme/typography',
                icon: 'cil-pencil',
                roles: [Role.Super]
            },
            {
                _name: 'CSidebarNavTitle',
                _children: ['Setari'],
                roles: [Role.Admin],
                icon: 'cil-puzzle',
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Utilizatori',
                to: '/users',
                icon: 'cil-list',
                roles: [Role.Admin]

            },
            {
                _name: 'CSidebarNavItem',
                name: 'Dispozitive',
                to: '/devices',
                icon: 'cil-list',
                roles: [Role.Admin]

            },
            {
                _name: 'CSidebarNavItem',
                name: 'Periferice',
                to: '/peripherals',
                icon: 'cil-list',
                roles: [Role.Admin]

            },
            {
                _name: 'CSidebarNavItem',
                name: 'Cabluri',
                to: '/cables',
                icon: 'cil-list',
                roles: [Role.Admin]

            },
            {
                _name: 'CSidebarNavTitle',
                _children: ['WUI'],
                roles: [Role.Admin],
                icon: 'cil-puzzle',
            },
            {
                _name: 'CSidebarNavItem',
                name: 'Parter',
                to: '/pages/wui',
                icon: 'cil-lightbulb',
                roles: [Role.Admin]

            }
        ]
    }
]
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
                roles: [Role.Super]
            },
            {
                _name: 'CSidebarNavDropdown',
                name: 'Utilizatori',
                route: '/users',
                icon: 'cil-puzzle',
                roles: [Role.Super],
                items: [
                    {
                        name: 'Lista utilizatorilor',
                        to: '/users',
                        icon: 'cil-puzzle',
                        roles: [Role.Super]

                    }
                ]
            }
        ]
    }
]
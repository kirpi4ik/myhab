<template>
    <CDropdown
            inNav
            class="c-header-nav-items"
            placement="bottom-end"
            add-menu-classes="pt-0"
    >
        <template #toggler>
            <CHeaderNavLink>
                <CIcon name="cil-list"/>
            </CHeaderNavLink>
        </template>
        <CDropdownHeader tag="div" class="text-center" color="light">
            <strong>Task-uri</strong>
        </CDropdownHeader>
        <CDropdownItem>
            <CIcon name="cil-bell"/>
            Updates
            <CBadge color="info" class="ml-auto">{{ itemsCount }}</CBadge>
        </CDropdownItem>
        <CDropdownItem>
            <CIcon name="cil-envelope-open"/>
            Messages
            <CBadge color="success" class="ml-auto">{{ itemsCount }}</CBadge>
        </CDropdownItem>
        <CDropdownItem>
            <CIcon name="cil-task"/>
            Tasks
            <CBadge color="danger" class="ml-auto">{{ itemsCount }}</CBadge>
        </CDropdownItem>
        <CDropdownItem>
            <CIcon name="cil-comment-square"/>
            Comments
            <CBadge color="warning" class="ml-auto">{{ itemsCount }}</CBadge>
        </CDropdownItem>
    </CDropdown>
</template>

<script>
    import {authenticationService} from '@/_services';
    import {router} from '@/_helpers';

    export default {
        name: 'TheHeaderDropdownTaskList',
        data() {
            return {
                itemsCount: 42
            }
        }, methods: {
            logout: function () {
                authenticationService.logout();
                router.push('/pages/login');
            }
        }
    }
</script>

<style scoped>
    .c-icon {
        margin-right: 0.3rem;
    }
</style>
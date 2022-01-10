<template>
    <div class="card" :class="`card-background text-white`">
        <div class="card-body pb-2">
            <slot></slot>
            <div>
                <font-awesome-icon icon="thermometer-half" size="3x" :class="`zone-icon-${peripheral.data.connectedTo[0].value < 20}`"/>
                <h4 class="mb-1"> {{peripheral.data.name}}</h4>
                <h4 class="mb-1">{{peripheral.data.connectedTo[0].value}}</h4>
            </div>
            <div v-if="hasRole(['ROLE_ADMIN'])">
                <CDropdown color="transparent p-0" placement="bottom-end" :ref="'dropdown-'+peripheral.data.id">
                    <template #toggler-content>
                        <CIcon name="cil-settings"/>
                    </template>
                    <CDropdownItem v-on:click="$router.push({path: '/peripherals/' + peripheral.data.id + '/view'})">Details</CDropdownItem>
                </CDropdown>
            </div>
        </div>
        <slot name="footer" class="card-footer">
            <div class="toggle-btn">
            </div>
        </slot>
    </div>
</template>

<script>
    import {authenticationService} from '@/_services';

    export default {
        name: 'PeriphTempControl',
        props: {
            peripheral: Object
        },
        methods:
            {
                hasRole: function (roles) {
                    const currentUser = authenticationService.currentUserValue;
                    return currentUser.permissions.filter(function (userRole) {
                        return roles.includes(userRole);
                    }).length > 0;
                }
            }

    }
</script>
<style scoped>
    .zone-card-background {
        background-image: linear-gradient(#4f6167, #8e949f);
    }

    .card-background {
        background-image: linear-gradient(#156cb8, #60a3c2);
    }

    .card-footer {
        text-align: center;
    }

    .zone-card-footer {
        height: 50pt;
    }

    .toggle-btn {
        margin: auto;
    }

    div.v-switch-core {
        border: 1px solid #000000 !important;
    }

    .zone-icon-false {
        float: right;
        color: yellow;
    }

    .zone-icon-true {
        float: right;
        fill: #044B9466;
        fill-opacity: 0.5;
    }
</style>

<template>
    <div class="card" :class="`card-background text-white`">
        <div class="card-body pb-2">
            <slot></slot>
            <div>
                <font-awesome-icon icon="thermometer-half" size="3x" :class="`zone-icon-${peripheral.portValue < 20}`"/>
                <h4 class="mb-1"> {{peripheral.data.name}}</h4>
                <h4 class="mb-1">{{peripheral.portValue}}</h4>
            </div>

        </div>
        <slot name="footer" class="card-footer">
            <div class="toggle-btn">
            </div>
        </slot>
    </div>
</template>

<script>
    import {router} from '@/_helpers';
    import {authenticationService} from '@/_services';
    import {PUSH_EVENT} from "../../graphql/zones";

    export default {
        name: 'PeriphLightControl',
        props: {
            peripheral: Object
        },
        methods: {
            periphStateChangeHandler: function (peripheral) {
                let event = {
                    "p0": "light",
                    "p1": "PERIPHERAL",
                    "p2": peripheral.data.uid,
                    "p3": "mweb",
                    "p4": peripheral.state === true ? "off" : "on",
                    "p6": authenticationService.currentUserValue.login
                };
                this.$apollo.mutate({
                    mutation: PUSH_EVENT, variables: {input: event}
                }).then(response => {

                });
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

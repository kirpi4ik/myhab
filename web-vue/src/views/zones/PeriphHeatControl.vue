<template>
    <div class="card" :class="`card-background-heat-${peripheral.state} text-white`">
        <div class="card-body pb-2">
            <slot></slot>
            <div>
                <font-awesome-icon icon="fire-alt" size="3x" :class="`zone-icon-${peripheral.state}`"/>
                <h4 class="mb-1"> {{peripheral.data.name}}</h4>
            </div>

        </div>
        <slot name="footer" class="card-footer">
            <div class="toggle-btn">
                <toggle-button v-model="peripheral.state" color="#82C7EB" :sync="true"
                               :labels="{checked: 'Porneste', unchecked: 'Opreste'}"
                               :switch-color="{checked: 'linear-gradient( #8DFF73, green)', unchecked: 'linear-gradient(#BF0000, #FFBE62)'}"
                               :color="{checked: '#809687', unchecked: '#b90000', disabled: '#CCCCCC'}"
                               :speed="300"
                               @change="periphStateChangeHandler(peripheral)"
                               :font-size="14"
                               :width="250"
                               :height="40"/>
            </div>
        </slot>
    </div>
</template>

<script>
    import {authenticationService} from '@/_services';
    import { PUSH_EVENT} from "../../graphql/zones";

    export default {
        name: 'PeriphHeatControl',
        props: {
            peripheral: Object
        },
        methods: {
            periphStateChangeHandler: function (peripheral) {
                let event = {
                    "p0": "heat",
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

    .card-background-heat-false {
        background-image: linear-gradient(#b8303c, #c25751);
    }
    .card-background-heat-true {
        background-image: linear-gradient(#b88586, #c27973);
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
        color: red;
    }

    .zone-icon-true {
        float: right;
        fill: #044B9466;
        fill-opacity: 0.5;
    }
</style>

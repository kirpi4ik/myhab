<template>
    <CRow>
        <template>
            <CCol md="3" sm="6" v-for="peripheral in peripherals" v-bind:key="peripheral.uid">
                <div class="card" :class="`card-background text-white`">
                    <div class="card-body pb-2">
                        <slot></slot>
                        <h4 class="mb-1"> {{peripheral.data.name}}</h4>

                    </div>
                    <slot name="footer" class="card-footer">
                        <div class="toggle-btn">
                            <toggle-button v-model="peripheral.state" color="#82C7EB" :sync="true"
                                           :labels="{checked: 'Aprinde', unchecked: 'Stinge'}"
                                           :switch-color="{checked: 'linear-gradient( #8DFF73, green)', unchecked: 'linear-gradient(#BF0000, #FFBE62)'}"
                                           :color="{checked: '#009663', unchecked: '#FF0000', disabled: '#CCCCCC'}"
                                           :speed="300"
                                           @change="periphStateChangeHandler({value:peripheral.state, id:peripheral.data.id, uid:peripheral.data.uid})"
                                           :font-size="14"
                                           :width="250"
                                           :height="40"/>
                        </div>
                    </slot>
                </div>
            </CCol>
        </template>
    </CRow>
</template>
<script>
    import {GET_ALL_ZONES, UPDATE_PORT_VALUE} from "../../graphql/zones";

    export default {
        name: 'ZoneLight',
        data() {
            return {
                zone: [],
                peripherals: []
            }
        },
        created() {
            this.loadInitial();
        },
        methods: {
            loadInitial() {
                this.$apollo.query({query: GET_ALL_ZONES, variables: {}}).then(response => {
                    this.zone = response.data.zoneList.filter(function (item) {
                        return item.parent == null
                    })[0];
                    const peripherals = []
                    this.zone.peripherals.forEach(function (peripheral) {
                        var state = false
                        if (peripheral.connectedTo.length > 0) {
                            state = peripheral.connectedTo[0].value === 'ON'
                        }
                        peripherals.push({data: peripheral, state: state});
                    });
                    this.peripherals = peripherals
                });
            },
            periphStateChangeHandler: function (value, srcEvent) {
                this.$apollo.mutate({
                    mutation: UPDATE_PORT_VALUE, variables: {id: 6816, portValue: {value: "OFF"}}
                }).then(response => {

                });
            }
        }
    }
</script>
<style scoped>
    .card-background {
        background-image: linear-gradient(#156cb8, #60a3c2);
    }

    .card-footer {
        text-align: center;
    }

    .toggle-btn {
        margin: auto;
    }

    div.v-switch-core {
        border: 1px solid #000000 !important;
    }
</style>
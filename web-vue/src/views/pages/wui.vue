<template>
    <div id="fullscreen">
        <div class="svg-container">
            <!--https://boxy-svg.com/app/disk:MtaL1PZN9k-->
            <inline-svg
                    src="svg/parter.svg"
                    :transformSource="transform"
                    fill-opacity="0.25"
                    :stroke-opacity="0.5"
                    :color="false"
                    ref="svg"
            ></inline-svg>
        </div>
        <CModal title="Introduceti codul de acces" color="warning" :show.sync="showPassword">
            <CForm validated novalidate>
                <CInput type="number" description="Please enter your password." placeholder="Cod deblocare" :value="unlockCode" @input="unlockCode =  $event" was-validated>
                    <template #append-content>
                        <CIcon name="cil-lock-unlocked"/>
                    </template>
                </CInput>
            </CForm>
            <template #footer>
                <CButton @click="unlock()" color="danger" type="submit">Deschide</CButton>
            </template>
        </CModal>
        <CModal
                title="Deblocarea a esuat"
                color="danger"
                :show.sync="showErrorModal"
        >
        </CModal>
    </div>
</template>
<script>
    import {authenticationService} from '@/_services';
    import {lightService} from '@/_services/controls';
    import InlineSvg from 'vue-inline-svg';
    import {PERIPHERAL_LIST_WUI, PUSH_EVENT} from "../../graphql/queries";
    import _ from "lodash";

    export default {
        components: {
            'inline-svg': InlineSvg
        },
        data() {
            return {
                peripheralLightUid: process.env.VUE_APP_CONF_PH_LIGHT_UID,
                peripheralLights: {},
                portToPeripheralMap: {},
                svgMap: {},
                nodes: ['path', 'polygon', 'polyline'],

                showPassword: false,
                showErrorModal: false,
                unlockCode: null
            }
        },
        created() {
            this.init()
        },
        mounted() {
            document.addEventListener('click', function (event) {
                console.log('click on light')

                if (event.target.id.startsWith("lock_")) {
                    console.log("UNLOCK")
                    this.unlockCode = null;
                    this.showPassword = true;
                } else {
                    let closest;
                    let i = 0
                    do {
                        closest = event.target.closest(this.nodes[i]);
                    } while (closest == null && i++ < this.nodes.length)
                    if (closest != null) {
                        let reverseCss = closest.getAttribute("class")
                        if (reverseCss == null) {
                            reverseCss = "no-focus"
                        }
                        closest.setAttribute("class", "focus")
                        setTimeout(function () {
                            closest.setAttribute("class", reverseCss)
                        }, 100)
                        console.log('FOUND: ' + closest.id)
                        let id = closest.id.split("_")[1];

                        if (this.peripheralLights[id] != null) {
                            console.log('TOGGLE: ' + this.peripheralLights[id].name)
                            lightService.toggle(this.peripheralLights[id])
                        }
                    }
                }
                console.log("RETURN");
            }.bind(this), false);
        },

        computed: {
            stompMessage() {
                return this.$store.state.stomp.message
            }
        },
        watch: {
            '$route.path': 'init',
            stompMessage: function (newVal) {
                if (newVal.eventName == 'evt_port_value_persisted') {
                    this.updatePeripheral(newVal.jsonPayload);
                }
            }
        },
        methods: {
            updatePeripheral: function (jsonPayload) {
                let payload = JSON.parse(jsonPayload);
                let connectedPeripherals = this.portToPeripheralMap[payload.p2]

                if (connectedPeripherals) {
                    connectedPeripherals.forEach(function (peripheralId) {
                        const peripheralComp = _.map(this.peripheralLights, (peripheral) => {
                            if (peripheral['id'] == peripheralId) {
                                peripheral['value'] = payload.p4;
                                peripheral['state'] = payload.p4 === 'ON';
                                return peripheral
                            }
                        })
                    }.bind(this))
                }
            },
            unlock: function () {
                let event = {
                    "p0": "evt_intercom_door_lock",
                    "p1": "PERIPHERAL",
                    "p2": this.peripheral.id,
                    "p3": "mweb",
                    "p4": "open",
                    "p5": '{"unlockCode": "' + this.unlockCode + '"}',
                    "p6": authenticationService.currentUserValue.login
                };
                this.$apollo.mutate({
                    mutation: PUSH_EVENT, variables: {input: event}
                }).then(response => {
                    this.showPassword = false
                });
            },
            init: function () {
                let peripheralLight = function (categoryToKeepId) {
                    return function (peripheral) {
                        return peripheral.category.uid === categoryToKeepId;
                    }
                    // return peripheral.data.category.uid === this.$route.query.categoryUid
                }.bind(this)
                let initPeripheralMap = function (peripheral) {
                    if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
                        let port = peripheral.connectedTo[0];
                        if (port != null) {
                            if (!this.portToPeripheralMap[port.id]) {
                                this.portToPeripheralMap[port.id] = []
                            }
                            this.portToPeripheralMap[port.id].push(peripheral.id)
                        }
                    }
                }.bind(this)
                let initState = function (peripheral) {
                    if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
                        let port = peripheral.connectedTo[0];

                        if (port != null) {
                            peripheral['portValue'] = port.value;
                            peripheral['state'] = peripheral['portValue'] === 'ON';
                            peripheral['portId'] = port.id;
                            peripheral['portUid'] = port.uid;
                        } else {
                            port = null;
                            state = false;
                            deviceState = null;
                        }

                        for (let node of this.nodes) {
                            Array.prototype.slice.call(document.getElementsByTagName(node)).filter(function (element) {
                                return element.id.lastIndexOf("id_" + peripheral.id, 0) === 0
                            }).forEach(function (path) {
                                if (peripheral['state']) {
                                    path.setAttribute("class", "bulb-on")
                                } else {
                                    path.setAttribute("class", "bulb-off")
                                }
                            });
                        }
                    }
                }.bind(this)
                this.$apollo.query({
                    query: PERIPHERAL_LIST_WUI,
                    variables: {},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    //clone response
                    let data = _.cloneDeep(response.data)
                    //convert to map
                    data.devicePeripheralList.forEach(initPeripheralMap)
                    let lights = data.devicePeripheralList.filter(peripheralLight(this.peripheralLightUid));
                    lights.forEach(initState)
                    this.peripheralLights = _.reduce(lights, function (hash, value) {
                        var key = value['id'];
                        hash[key] = value;
                        return hash;
                    }, {});


                });
            },
            transform: function (svg) {
                this.svgMap = svg;
                let svgForeignObjectElement = document.createElementNS("http://www.w3.org/2000/svg", 'foreignObject');
                svgForeignObjectElement.setAttribute("x", 0); //Set rect data
                svgForeignObjectElement.setAttribute("y", 0); //Set rect data
                svgForeignObjectElement.setAttribute("width", "100"); //Set rect data
                svgForeignObjectElement.setAttribute("height", "50"); //Set rect data

                var buttonElement = document.createElement('a');
                buttonElement.innerText = 'Back'
                buttonElement.setAttribute("class", 'btn btn-success back');
                buttonElement.href = "/#/"
                svgForeignObjectElement.appendChild(buttonElement);

                svg.appendChild(svgForeignObjectElement);

                for (const node of this.nodes) {
                    let elementsByTagName = svg.getElementsByTagName(node);
                    for (var i = 0; i < elementsByTagName.length; i++) {
                        this.wrap(svg, elementsByTagName[i])
                    }
                }

                return svg;
            },
            wrap: function (svg, pathEl) {
                let wrapper = document.createElementNS("http://www.w3.org/2000/svg", 'a')
                // wrapper.setAttribute("xlink:href", "#0");
                let bulbClass = "bulb-off";
                let light = this.peripheralLights[pathEl.id.split("_")[1]];
                if (light && light.state) {
                    bulbClass = "bulb-on"
                }
                pathEl.setAttribute("class", bulbClass);
                pathEl.parentNode.insertBefore(wrapper, pathEl);
                wrapper.appendChild(pathEl);
            },
            openLock: function () {
                debugger
                alert('sss')
            }

        }
    }

    function openLock() {
        alert('xxx')
    }
</script>
<style>
    body {
    }

    .back {
        opacity: 0.8;
        color: #000015;
    }

    .focus {
        fill: green;
        stroke: yellow;
        stroke-width: 1;
    }

    .no-focus {
        stroke: none;
    }

    .bulb-on {
        fill: #d6d40f;
        fill-opacity: 0.7;
    }

    .bulb-off {
        fill: #4a90d6;
        fill-opacity: 0.40;
        stroke: #2b6095;
        stroke-width: 0.5;
    }
    circle#lock_circle{
        stroke: #d3e5e5;
        stroke-width: 1.2;
        fill: rgb(98, 117, 129);
        paint-order: stroke;
        fill-opacity: 0.59;
    }
</style>
<template>
  <!--https://boxy-svg.com/app/disk:MtaL1PZN9k-->
  <div id="fullscreen">
    <swiper :pagination="{ dynamicBullets: true }" :modules="modules" class="swiper">
      <swiper-slide v-for="svgPage in this.svgPages" v-bind:key="svgPage.id">
        <inline-svg
          :src="svgPage.svgContent"
          :transformSource="transform"
          :fill-opacity="svgPage.fillOpacity"
          :stroke-opacity="svgPage.strokeOpacity"
          :color="false"
          ref="svg"
        ></inline-svg>
      </swiper-slide>
    </swiper>
    <q-dialog v-model="unlockConfirmation.show" transition-show="jump-up" transition-hide="jump-down">
      <q-card class="bg-white">
        <q-bar class="bg-deep-orange-7 text-white">
          <q-icon name="lock"/>
          <div>Alert</div>
          <q-space/>
          <q-btn dense flat icon="close" v-close-popup>
            <q-tooltip class="bg-primary">Close</q-tooltip>
          </q-btn>
        </q-bar>
        <q-card-section>
          <div class="text-h6">Atentie! Doriti sa deschideti ?</div>
        </q-card-section>

        <q-card-section class="q-pa-none" vertical align="center">
          <div class="q-pa-sm">
            <q-btn flat class="text-h6" icon="fas fa-lock-open" label="Deschide" no-caps @click="unlock"></q-btn>
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>
  </div>
</template>
<script>
import {Swiper, SwiperSlide} from 'swiper/vue';
import {Pagination} from 'swiper';
import 'swiper/css';

import 'swiper/css/pagination';
import {authzService} from '@/_services';
import {heatService, lightService} from '@/_services/controls';
import InlineSvg from 'vue-inline-svg';
import {PERIPHERAL_LIST_WUI, PUSH_EVENT} from '@/graphql/queries';
import _ from 'lodash';

export default {
  name: 'MobileWebLayout',
  components: {
    'inline-svg': InlineSvg,
    Swiper,
    SwiperSlide,
  },
  setup() {
    return {
      modules: [Pagination],
    };
  },
  data() {
    return {
      srvPeripherals: {},
      portToPeripheralMap: {},
      assetMap: {},
      svgMap: {},
      svgPages: [
        {
          id: 'screen-1',
          visible: true,
          svgContent: 'svg/screen-1.svg',
          fillOpacity: 0.3,
          strokeOpacity: 0.5,
        },
        {
          id: 'screen-2',
          visible: false,
          svgContent: 'svg/screen-2.svg',
          fillOpacity: 0.3,
          strokeOpacity: 0.5,
        },
      ],
      nodes: ['path', 'rect', 'circle', 'polygon', 'polyline', 'text', 'g'],
      unlockConfirmation: {
        show: false,
        unlockCode: null,
        assetId: null,
      },
      showErrorModal: false,
    };
  },
  created() {
    this.init();
  },
  mounted() {
    document.addEventListener('click', this.handleClick, false);
  },

  computed: {
    stompMessage() {
      return this.$store.state.ws.message;
    },
  },
  watch: {
    '$route.path': 'init',
    stompMessage: function (newVal) {
      if (newVal.eventName === 'evt_port_value_persisted') {
        this.updatePeripheralUI(newVal.jsonPayload);
      }
    },
  },
  methods: {
    assetIdParser: function (id) {
      let svgElement = [...id.matchAll(/([A-Za-z_]+)-(([0-9]+)(-([0-9])))*/g)];
      return {
        type: svgElement[0] != null ? svgElement[0][1] : null,
        info: svgElement[1] != null ? svgElement[1][1] : null,
        category: svgElement[1] != null ? svgElement[1][1].toUpperCase() : null,
        id: svgElement[2] != null ? svgElement[2][3] : null,
        assetOrder: svgElement[2] != null ? svgElement[2][5] : null,
      };
    },
    handleClick: function (event) {
      let targetId = event.target.id;
      if (targetId.startsWith('nav-')) {
        let direction = this.assetIdParser(targetId).info;
        if (direction === 'home') {
          this.$router.push({path: '/'}).catch(() => {
          });
        } else if (direction === 'back' || direction === 'forward') {
          if (direction === 'forward' && this.svgPageHasNext()) {
            this.svgPageGoNext();
          } else if (direction === 'back' && this.svgPageHasPrev()) {
            this.svgPageGoBack();
          }
        }
      } else if (targetId.startsWith('asset-')) {
        let closest;
        let i = 0;
        do {
          closest = event.target.closest(this.nodes[i]);
        } while (closest == null && i++ < this.nodes.length);
        if (closest != null) {
          let asset = this.assetIdParser(closest.id);
          let reverseCss = closest.getAttribute('class');
          if (reverseCss == null) {
            reverseCss = 'no-focus';
          }
          closest.setAttribute('class', 'focus');
          setTimeout(function () {
            closest.setAttribute('class', reverseCss);
          }, 100);
          let id = asset['id'];
          if (this.srvPeripherals[id] != null) {
            switch (this.srvPeripherals[id].category.name) {
              case 'DOOR_LOCK': {
                this.unlockConfirmation.show = true;
                this.unlockConfirmation.assetId = id;
                this.unlockConfirmation.unlockCode = null;
                break;
              }
              case 'LIGHT': {
                lightService.toggle(this.srvPeripherals[id]);
                break;
              }
              case 'HEAT': {
                heatService.toggle(this.srvPeripherals[id]);
                break;
              }
            }
          } else {
            console.log('null id');
          }
        }
      }
    },
    updatePeripheralUI: function (jsonPayload) {
      let payload = JSON.parse(jsonPayload);
      let connectedPeripherals = this.portToPeripheralMap[payload.p2];

      if (connectedPeripherals) {
        connectedPeripherals.forEach(
          function (peripheralId) {
            const peripheralComp = _.map(this.srvPeripherals, peripheral => {
              if (peripheral['id'] === peripheralId) {
                peripheral['portValue'] = payload.p4;
                peripheral['state'] = payload.p4 === 'ON';
                return peripheral;
              }
            });
          }.bind(this),
        );
      }
    },
    unlock: function () {
      let event = {
        p0: 'evt_intercom_door_lock',
        p1: 'PERIPHERAL',
        p2: this.unlockConfirmation.assetId,
        p3: 'mweb',
        p4: 'open',
        p5: `{"unlockCode": "${this.unlockConfirmation.unlockCode}"}`,
        p6: authzService.currentUserValue.login,
      };
      this.$apollo
        .mutate({
          mutation: PUSH_EVENT,
          variables: {input: event},
        })
        .then(response => {
          this.unlockConfirmation = false;
        });
    },
    init: function () {
      let initPeripheralMap = function (peripheral) {
        if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
          let port = peripheral.connectedTo[0];
          if (port != null) {
            if (!this.portToPeripheralMap[port.id]) {
              this.portToPeripheralMap[port.id] = [];
            }
            this.portToPeripheralMap[port.id].push(peripheral.id);
            let category = peripheral.category.name.toLowerCase();
            if (!this.assetMap[category]) {
              this.assetMap[category] = [];
            }
            if (!this.assetMap[category][port.id]) {
              this.assetMap[category][port.id] = [];
            }
            this.assetMap[category][port.id].push(peripheral.id);
          }
        }
      }.bind(this);
      let initState = function (peripheral) {
        if (peripheral.connectedTo && peripheral.connectedTo.length > 0) {
          let port = peripheral.connectedTo[0];

          if (port != null) {
            peripheral['portValue'] = port.value;
            peripheral['state'] = peripheral['portValue'] === 'ON';
            peripheral['portId'] = port.id;
            peripheral['portUid'] = port.uid;
            peripheral['deviceStatus'] = port.device.status;
          } else {
            port = null;
            state = false;
            deviceState = null;
          }
        }
      }.bind(this);

      this.$apollo
        .query({
          query: PERIPHERAL_LIST_WUI,
          variables: {},
          fetchPolicy: 'network-only',
        })
        .then(response => {
          //clone response
          let data = _.cloneDeep(response.data);
          //convert to map
          data.devicePeripheralList.forEach(initPeripheralMap);
          let assets = data.devicePeripheralList;
          assets.forEach(initState);
          this.srvPeripherals = _.reduce(
            assets,
            function (hash, value) {
              var key = value['id'];
              hash[key] = value;
              return hash;
            },
            {},
          );
        });
    },
    transform: function (svg) {
      this.svgMap = svg;
      for (const node of this.nodes) {
        let elementsByTagName = svg.getElementsByTagName(node);
        for (let i = 0; i < elementsByTagName.length; i++) {
          this.svgElInit(svg, elementsByTagName[i]);
        }
      }

      return svg;
    },
    svgElInit: function (svg, svgEl) {
      let wrapper = document.createElementNS('http://www.w3.org/2000/svg', 'a');
      let actionElementClass = '';
      let svgElement = this.assetIdParser(svgEl.id);
      if (svgElement.type === 'asset') {
        let srvAsset = this.srvPeripherals[svgElement['id']];
        switch (svgElement['category']) {
          case 'LIGHT': {
            if (srvAsset && srvAsset.state) {
              actionElementClass = 'bulb-on';
            } else {
              actionElementClass = 'bulb-off';
            }
            break;
          }
          case 'HEAT': {
            if (srvAsset && srvAsset.state) {
              actionElementClass = 'heat-on';
            } else {
              actionElementClass = 'heat-off';
            }
            break;
          }
          case 'MOTION': {
            if (srvAsset && srvAsset.state) {
              actionElementClass = 'motion-off';
            } else {
              actionElementClass = 'motion-on';
            }
            break;
          }
          case 'TEMP': {
            if (srvAsset && srvAsset.portValue) {
              let degree = srvAsset.portValue
              if (degree.length == 3 && degree.indexOf(".") == -1) {
                degree = degree / 10
              }
              svgEl.firstChild.textContent = degree + '℃';
            }
            if (srvAsset && srvAsset['deviceStatus'] === 'OFFLINE') {
              actionElementClass = 'device-offline';
            }
            break;
          }
          case 'DOOR_LOCK': {
            actionElementClass = 'lock';
            break;
          }
          case 'LUMINOSITY': {
            if (srvAsset && srvAsset.portValue) {
              svgEl.getElementsByTagName('text').item(0).textContent = srvAsset.portValue / 10 + '%';
            }
            svgEl.getElementsByTagName('text').item(0).setAttribute('class', 'luminosity-text');
            break;
          }
        }
      } else if (svgElement.type === 'nav') {
        if (svgElement.info === 'back' || svgElement.info === 'forward') {
          actionElementClass = 'nav-button';
          if (svgElement.info === 'back' && svg.id === this.svgPages[0].id) {
            actionElementClass = 'hidden';
          } else if (svgElement.info === 'forward' && svg.id === this.svgPages[this.svgPages.length - 1].id) {
            actionElementClass = 'hidden';
          }
        } else {
          actionElementClass = 'bulb-off';
        }
      }
      svgEl.setAttribute('class', actionElementClass);
      svgEl.parentNode.insertBefore(wrapper, svgEl);
      wrapper.appendChild(svgEl);
    },
    svgPageHasNext: function () {
      let currentIndex = _.findIndex(
        this.svgPages,
        e => {
          return e.visible === true;
        },
        0,
      );
      return currentIndex + 1 < this.svgPages.length;
    },
    svgPageHasPrev: function () {
      let currentIndex = _.findIndex(
        this.svgPages,
        e => {
          return e.visible === true;
        },
        0,
      );
      return currentIndex - 1 >= 0;
    },
    svgPageGoNext: function () {
      let currentIndex = _.findIndex(
        this.svgPages,
        e => {
          return e.visible === true;
        },
        0,
      );

      this.svgPages[currentIndex + 1].visible = true;
      this.svgPages[currentIndex].visible = false;
    },
    svgPageGoBack: function () {
      let currentIndex = _.findIndex(
        this.svgPages,
        e => {
          return e.visible === true;
        },
        0,
      );

      this.svgPages[currentIndex - 1].visible = true;
      this.svgPages[currentIndex].visible = false;
    },
  },
};
</script>
<style>
.hidden {
  display: none;
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

rect.nav-button {
  stroke: #2b6095;
  stroke-width: 0.5;
  fill: rgb(115, 127, 129);
  fill-rule: nonzero;
  fill-opacity: 0.3;
  paint-order: stroke;
}

path.nav-button {
  stroke: #2b6095;
  stroke-width: 0.5;
  fill: rgb(115, 127, 129);
  fill-rule: nonzero;
}

.motion-off {
  fill: #5a99de;
  fill-opacity: 0.3;
  stroke: #70808e;
  stroke-width: 0.8;
}

.motion-on {
  fill: #e8b8bc;
  fill-opacity: 0.3;
  stroke: #ba1334;
  stroke-width: 0.8;
}

.bulb-on {
  fill: #d6d40f;
  fill-opacity: 0.7;
}

.bulb-off {
  fill: #4a90d6;
  fill-opacity: 0.3;
  stroke: #2b6095;
  stroke-width: 0.5;
}

.device-offline {
  color: red;
  fill: rgb(88, 77, 77);
  stroke: rgb(226, 9, 9);
  text-decoration: line-through;
}

circle.heat-on {
  fill: rgb(244, 194, 168);
  fill-opacity: 0.61;
  stroke: rgb(116, 29, 29);
}

path.heat-on {
  paint-order: fill;
  stroke-width: 4.62558px;
  fill: rgb(88, 77, 77);
  stroke: rgb(226, 9, 9);
  stroke-opacity: 0.34;
}

circle.heat-off {
  fill: rgb(168, 193, 244);
  fill-opacity: 0.61;
  stroke: rgb(116, 29, 29);
}

path.heat-off {
  paint-order: fill;
  stroke-width: 4.62558px;
  fill: rgb(88, 77, 77);
  stroke: rgb(9, 96, 226);
  stroke-opacity: 0.34;
}

.lock {
  cursor: pointer;
  fill: #4a90d6;
  fill-opacity: 0.3;
  stroke: #d3e5e5;
  stroke-width: 1.2;
}

circle.asset-lock-circle {
  stroke: #d3e5e5;
  stroke-width: 1.2;
  fill: rgb(98, 117, 129);
  paint-order: stroke;
  fill-opacity: 0.59;
}

circle#nav-home-1 {
  stroke: #d3e5e5;
  stroke-width: 1.2;
  fill: rgb(98, 117, 129);
  paint-order: stroke;
  fill-opacity: 0.59;
}

text.txt-light {
  fill: rgb(224, 227, 243);
  font-family: Arial, sans-serif;
  font-size: 113.1px;
  fill-opacity: 0.8;
}

text.luminosity-text {
  fill: #d3e5e5;
  fill-opacity: 0.9;
}

/* Enter and leave animations can use different */
/* durations and timing functions.              */
.slide-fade-enter-active {
  transition: all 0.3s ease;
}

.slide-fade-leave-active {
  transition: all 0.3s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter, .slide-fade-leave-to
  /* .slide-fade-leave-active below version 2.1.8 */
{
  transform: translateX(10px);
  opacity: 0;
}

.swiper {
  width: 100%;
  height: 100%;
}

.swiper-slide {
  text-align: center;
  font-size: 18px;
  background: #fff;

  /* Center slide text vertically */
  display: -webkit-box;
  display: -ms-flexbox;
  display: -webkit-flex;
  display: flex;
  -webkit-box-pack: center;
  -ms-flex-pack: center;
  -webkit-justify-content: center;
  justify-content: center;
  -webkit-box-align: center;
  -ms-flex-align: center;
  -webkit-align-items: center;
  align-items: center;
}

.swiper-slide img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>

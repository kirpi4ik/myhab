<template>
  <div style="display: inline">
    <div v-for="port in portIds" style="display: inline-block" class="temp_display" v-bind:key="port.id"> {{port.value}} &#176;C</div>
  </div>
</template>
<script>
  export default {
    name: 'TempDisplay',
    props: {
      zone: Object
    },
    setup(props) {
      let portIds = [];
      
      // Add null checks for peripherals
      if (props.zone?.peripherals) {
        props.zone.peripherals.forEach(function (peripheral, index) {
          if (peripheral?.category?.name === 'TEMP') {
            if (peripheral.connectedTo) {
              peripheral.connectedTo.forEach(function (port, index) {
                portIds.push(port);
              });
            }
          }
        });
      }
      
      return {
        portIds
      }
    }
  }
</script>
<style scoped>
  div.temp_display {
    margin-bottom: 2pt;
    margin-left: 2pt;
    padding: 5pt;
    color: #74f0a2;
    text-decoration-color: #afb2c9;
    background-color: #818792;
    border: 1px solid rgba(0, 0, 0, 0.3);;
  }
</style>

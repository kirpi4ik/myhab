<template>
    <div>
        <ZoneActionListControl/>
    </div>
</template>

<script>
    import ZoneActionListControl from '../widgets/zoneaction/ZoneActionListControl'
    import TempChartControl from './TempChartControl'

    export default {
        name: 'Dashboard',
        components: {
            ZoneActionListControl,
            TempChartControl
        }
    }
</script>

<template>
    <CFooter>
        <div>
            <a href="https://madhouse.app" target="_self">MadHouse v{{version}}</a>
            <span class="ml-1">&copy; 2020 DevExpert.</span>
        </div>
        <div class="ml-auto">
            <span class="mr-1">Powered by</span>
            <a href="https://madhouse.app" target="_self">MadHouse &trade;</a>
        </div>
    </CFooter>
</template>

<script>
    export default {
        name: 'TheFooter',
        data() {
            return {
                version: process.env.VUE_APP_CONF_VERSION
            };
        },
    }
</script>

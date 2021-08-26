<template>
    <CFooter>
        <div>
            <a href="https://madhouse.app" target="_self">MadHouse v{{version}} <small><i>[ {{ date }} ]</i></small></a>
            <span class="ml-1">&copy; 2021 DevExpert.</span>
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
                version: process.env.VUE_APP_CONF_VERSION,
                date : process.env.VUE_APP_CONF_VERSION_BUILD_DATE
            };
        },
    }
</script>

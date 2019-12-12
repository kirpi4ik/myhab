<template>
    <div class="card">
        <div class="card-header content-center text-white p-2" :class="[`header-color-${color}`, addHeaderClasses]">
            <slot/>
            <font-awesome-icon :icon="['fas', `${icon}`]" size="3x"/>

        </div>
        <slot name="body" class="card-footer">
            <div class="card-body row text-center">
                <div class="col" v-on:click="lightNav(zoneUidLeft, categoryUid)">
                    <div v-if="leftHeader" class="text-value-lg">
                        {{leftHeader}}
                    </div>
                    <div v-if="leftFooter" class="text-uppercase text-muted small">
                        {{leftFooter}}
                    </div>
                </div>
                <div class="c-vr"></div>
                <div class="col" v-on:click="lightNav(zoneUidRight, categoryUid)">
                    <div v-if="rightHeader" class="text-value-lg">
                        {{rightHeader}}
                    </div>
                    <div v-if="rightFooter" class="text-uppercase text-muted small">
                        {{rightFooter}}
                    </div>
                </div>
            </div>
        </slot>
    </div>
</template>

<script>
    import {router} from '@/_helpers';

    export default {
        name: 'ZoneActionControl',
        props: {
            icon: String,
            color: String,
            rightHeader: String,
            rightFooter: String,
            leftHeader: String,
            leftFooter: String,
            zoneUidLeft: String,
            zoneUidRight: String,
            categoryUid: String,
            addHeaderClasses: [String, Array, Object]
        },
        methods: {
            lightNav: function (zoneUid, categoryUid) {
                router.push({path: 'zones', query: {zoneUid: zoneUid, categoryUid: categoryUid}})
            }
        }
    }
</script>
<style scoped>
    .header-color-light {
        background-color: #e0b351 !important;
    }

    .header-color-heat {
        background-color: #e0550d !important;
    }

    .header-color-temp {
        background-color: #4d80e0 !important;
    }
    .card-footer {
        border-color: #edf3fa !important;
    }
</style>

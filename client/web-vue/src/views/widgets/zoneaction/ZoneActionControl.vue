<template>
    <div class="card">
        <div class="card-header content-center text-white p-2" :class="[`header-color-${color}`, addHeaderClasses]">
            <slot>
            </slot>
            <div class="header-label">{{header}}</div>
            <div class="header-icon">
                <font-awesome-icon :icon="['fas', `${icon}`]" size="3x"/>
            </div>

        </div>
        <slot name="body" class="card-footer">
            <div class="card-body row text-center">
                <div class="col" v-on:click="lightNav(zoneIdLeft, category)" style="cursor: pointer">
                    <div v-if="leftHeader" class="text-value-lg">
                        {{leftHeader}}
                    </div>
                    <div v-if="leftFooter" class="text-uppercase text-muted small">

                    </div>
                </div>
                <div class="c-vr"></div>
                <div class="col" v-on:click="lightNav(zoneIdRight, category)" style="cursor: pointer">
                    <div v-if="rightHeader" class="text-value-lg">
                        {{rightHeader}}
                    </div>
                    <div v-if="rightFooter" class="text-uppercase text-muted small">

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
            header: String,
            icon: String,
            color: String,
            rightHeader: String,
            rightFooter: String,
            leftHeader: String,
            leftFooter: String,
            zoneIdLeft: String,
            zoneIdRight: String,
            category: String,
            addHeaderClasses: [String, Array, Object]
        },
        methods: {
            lightNav: function (zoneId, category) {
                router.push({path: `zones/${zoneId}`, query: {category: category}})
            }
        }
    }
</script>
<style scoped>
    .card-header {
        width: 100%;
        display: inline;
    }

    .header-label {
        float: left;
        color: rgba(255, 255, 255, 0.6);
        font-size: 18pt;
        font-weight: bold;
        margin-left: 10pt;
    }

    .header-icon {
        float: right;
        fill-opacity: 0.3;
        margin-right: 10pt;
    }

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

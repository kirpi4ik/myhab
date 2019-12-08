<template>
    <CRow>
        <template>
            <CCol md="3" sm="6" v-for="(zone, index) in zoneList">
                <CWidgetBrand
                        color="facebook"
                        :right-header="zone.description"
                        right-footer="ccccc"
                        :left-header="zone.name"
                        left-footer="yyyyy">
                    >
                    <CIcon
                            name="cib-facebook"
                            height="52"
                            class="my-4"
                    />

                </CWidgetBrand>
            </CCol>
        </template>
    </CRow>
</template>

<script>

    export default {
        name: 'WidgetsBrand',
        data() {
            return {
                zoneList: null
            }
        },
        created() {
            this.loadInitial();
        },
        methods: {
            loadInitial() {
                this.$apollo.query({
                    query: require("../../graphql/allUsers.graphql"),
                    variables: {}
                })
                    .then(response => {
                        this.zoneList = response.data.zoneList.filter(function (item) {
                            return item.parent == null
                        });
                    });
            }
        }
    }
</script>

<style scoped>
    .c-chart-brand {
        position: absolute;
        width: 100%;
        height: 100px;
    }
</style>

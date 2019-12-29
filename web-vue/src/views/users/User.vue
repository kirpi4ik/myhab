<template>
    <CRow>
        <CCol col="12" lg="6">
            <CCard>
                <CCardHeader>
                    <strong>User details : <span class="field-value">{{ user.name }}</span> </strong>
                    <div class="card-header-actions" @click="rowClicked">
                        <a style="cursor: pointer" class="card-header-action" rel="noreferrer noopener">
                            <small class="text-muted">edit</small>
                        </a>
                    </div>
                </CCardHeader>
                <CCardBody>
                    <CDataTable
                            striped
                            hover
                            small
                            fixed
                            :items="userDetails"
                            :fields="$options.fields"
                    />
                </CCardBody>
                <CCardFooter>
                    <CButton color="primary" @click="goBack">Back</CButton>
                </CCardFooter>
            </CCard>
        </CCol>
    </CRow>
</template>

<script>
    import {USER_GET_BY_ID} from "../../graphql/zones";


    export default {
        name: 'User',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        data: () => {
            return {
                userDetails: [],
                readonly: ["id", "__typename", "uid", "name"],
                user: {}
            }
        },
        mounted() {
            this.loadUser();
        },
        watch: {
            '$root.componentKey': 'loadUser'
        },
        methods: {
            loadUser() {
                this.loading = true
                let user = {};
                let removeReadonly = function (keyMap) {
                    return !this.readonly.includes(keyMap.key)
                }.bind(this);

                this.$apollo.query({
                    query: USER_GET_BY_ID,
                    variables: {uid: this.$route.params.id},
                    fetchPolicy: 'network-only'
                }).then(response => {
                    user = response.data.userByUid;
                    const userDetailsToMap = user ? Object.entries(user) : [['id', 'Not found']]
                    this.userDetails = userDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    }).filter(removeReadonly)
                    this.user = user
                });
                this.loading = false
            },
            rowClicked(item, index) {
                this.$router.push({path: "/users/" + this.$route.params.id + "/edit"})
            },
            goBack() {
                this.$router.go(-1)
            }
        }
    }
</script>
<style>
    .field-value {
        color: #abb3c0;
    }
</style>
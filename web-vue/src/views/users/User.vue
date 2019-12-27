<template>
    <CRow>
        <CCol col="12" lg="6">
            <CCard>
                <CCardHeader>
                    User id: {{ $route.params.id }}
                </CCardHeader>
                <CCardBody>
                    <CDataTable
                            striped
                            dark
                            hover
                            small
                            fixed
                            :items="userDetails"
                            :fields="$options.fields"
                            @row-clicked="rowClicked"
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
    import {router} from '@/_helpers';


    export default {
        name: 'User',
        fields: [
            {key: 'key', _style: 'width:150px'},
            {key: 'value', _style: 'width:150px;'}
        ],
        name: 'Users',
        data: () => {
            return {
                userDetails: []
            }
        },
        created() {
            this.loadUsers();
        },
        methods: {
            loadUsers() {
                let user = []
                this.$apollo.query({
                    query: USER_GET_BY_ID,
                    variables: {uid: this.$route.params.id}
                }).then(response => {
                    user = response.data.userByUid;
                    const userDetailsToMap = user ? Object.entries(user) : [['id', 'Not found']]
                    this.userDetails = userDetailsToMap.map(([key, value]) => {
                        return {key, value}
                    })
                });
            },
            rowClicked(item, index) {
                this.$router.push({path: this.$route.params.id + "/edit"})
            },
            goBack() {
                this.$router.go(-1)
            }
        }
    }
</script>

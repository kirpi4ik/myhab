<template>
    <div>
        <CRow>
            <CCol sm="12">
                <CInput
                        label="Name"
                        placeholder="Enter your name"
                />
            </CCol>
        </CRow>
        <CRow>
            <CCol sm="12">
                <CInput
                        label="Credit Card Number"
                        placeholder="0000 0000 0000 0000"
                />
            </CCol>
        </CRow>
        <CRow>
            <CCol sm="4">
                <CSelect
                        label="Month"
                        :options="[1,2,3,4,5,6,7,8,9,10,11,12]"
                />
            </CCol>
            <CCol sm="4">
                <CSelect
                        label="Year"
                        :options="[2014,2015,2016,2017,2018,2019,2020,2021,2022,2023,2024,2025]"
                />
            </CCol>
            <CCol sm="4">
                <CInput
                        label="CVV/CVC"
                        placeholder="123"
                />
            </CCol>
        </CRow>
    </div>
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

<template>
    <CContainer class="d-flex align-items-center min-vh-100">
        <CRow class="justify-content-center">
            <CCol md="8">
                <CCardGroup>
                    <CCard class="p-4">
                        <CCardBody>
                          <form @submit.prevent="onSubmit">
                            <div class="form-group">
                              <label for="username">Username</label>
                              <input type="text" v-model.trim="$v.username.$model" name="username" class="form-control" :class="{ 'is-invalid': submitted && $v.username.$error }" />
                              <div v-if="submitted && !$v.username.required" class="invalid-feedback">Username is required</div>
                            </div>
                            <div class="form-group">
                              <label htmlFor="password">Password</label>
                              <input type="password" v-model.trim="$v.password.$model" name="password" class="form-control" :class="{ 'is-invalid': submitted && $v.password.$error }" />
                              <div v-if="submitted && !$v.password.required" class="invalid-feedback">Password is required</div>
                            </div>
                            <div class="form-group">
                              <button class="btn btn-primary" :disabled="loading">
                                <span class="spinner-border spinner-border-sm" v-show="loading"></span>
                                <span>Login</span>
                              </button>
                            </div>
                            <div v-if="error" class="alert alert-danger">{{error}}</div>
                          </form>
                        </CCardBody>
                    </CCard>
                    <CCard
                            color="primary"
                            text-color="white"
                            class="text-center py-5 d-md-down-none"
                            style="width:44%"
                            body-wrapper
                    >
                        <h2>Sign up</h2>
                        <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut
                            labore et dolore magna aliqua.</p>
                        <CButton
                                color="primary"
                                class="active mt-3"
                                onsubmit="alert('sds')"
                        >
                            Register Now!
                        </CButton>
                    </CCard>
                </CCardGroup>
            </CCol>
        </CRow>
    </CContainer>
</template>

<script>
    import {required} from 'vuelidate/lib/validators';

    import {router} from '@/_helpers';
    import {authenticationService} from '@/_services';

    export default {
        data() {
            return {
                username: '',
                password: '',
                submitted: false,
                loading: false,
                returnUrl: '',
                error: ''
            };
        },
        validations: {
            username: {required},
            password: {required}
        },
        created() {
            // redirect to home if already logged in
            if (authenticationService.currentUserValue) {
                return router.push('/');
            }

            // get return url from route parameters or default to '/'
            this.returnUrl = this.$route.query.returnUrl || '/';
        },
        methods: {
            onSubmit() {
                this.submitted = true;

                // stop here if form is invalid
                // this.$v.$touch();
                // if (this.$v.$invalid) {
                //     return;
                // }

                this.loading = true;
                authenticationService.login(this.username, this.password)
                    .then(
                        user => router.push(this.returnUrl),
                        error => {
                          debugger
                            this.error = error;
                            this.loading = false;
                        }
                    );
            }
        }
    };
</script>

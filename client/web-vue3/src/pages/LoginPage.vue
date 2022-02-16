<template>
  <q-layout>
    <q-page-container>
      <q-page class="flex bg-image flex-center">
        <q-card v-bind:style="$q.screen.lt.sm ? { width: '80%' } : { width: '30%' }">
          <q-card-section>
            <q-avatar size="103px" class="absolute-center shadow-10">
              <img src="profile.svg"/>
            </q-avatar>
          </q-card-section>
          <q-card-section>
            <div class="text-center q-pt-lg">
              <div class="col text-h6 ellipsis">Log in</div>
            </div>
          </q-card-section>
          <q-card-section>
            <q-form class="q-gutter-md" @submit="login">
              <q-input filled v-model="username" label="Username" lazy-rules/>
              <q-input type="password" filled v-model="password" label="Password" lazy-rules/>
              <div>
                <q-btn label="Login" type="submit" color="secondary"/>
              </div>
            </q-form>
          </q-card-section>
        </q-card>
      </q-page>
    </q-page-container>
  </q-layout>
</template>

<script>
  import {useRoute} from 'vue-router';
  import {defineComponent, ref} from 'vue';
  import {authenticationService} from '@/_services';
  import {mapActions} from 'vuex';


  export default defineComponent({
    setup() {
      let returnUrl = ref(useRoute().query.returnUrl || '/');
      return {
        username: ref(''),
        password: ref(''),
        returnUrl,
      };
    },
    methods: {
      ...mapActions(['connect']),
      login() {
        this.loading = true;
        authenticationService.login(this.username, this.password).then(
          user => {
            this.connect()
            this.$router.push(this.returnUrl)
          },
          error => {
            this.error = error;
            this.loading = false;
          },
        );
      },
    },
  });
</script>

<style>
  .bg-image {
    background-image: linear-gradient(135deg, #7028e4 0%, #e5b2ca 100%);
  }
</style>

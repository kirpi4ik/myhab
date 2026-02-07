<template>
  <div class="offline-page">
    <div class="offline-content">
      <q-icon name="mdi-wifi-off" size="120px" color="grey-5"/>
      <h1 class="offline-title">You're Offline</h1>
      <p class="offline-message">
        It looks like you've lost your internet connection.
        <br>
        Please check your connection and try again.
      </p>
      <q-btn
        label="Try Again"
        icon="mdi-refresh"
        color="primary"
        unelevated
        @click="reload"
        class="q-mt-md"
      />
      <div class="offline-info q-mt-xl">
        <q-icon name="mdi-information" size="20px" class="q-mr-xs"/>
        <span>Some features may be available offline</span>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Reload the page
 */
const reload = () => {
  globalThis.location.reload();
};
</script>

<style scoped lang="scss">
.offline-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e9f2 100%);
  padding: 20px;
}

.offline-content {
  text-align: center;
  max-width: 500px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.offline-title {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  margin: 24px 0 16px 0;
}

.offline-message {
  font-size: 16px;
  color: #6b7280;
  line-height: 1.6;
  margin-bottom: 24px;
}

.offline-info {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #9ca3af;
}
</style>


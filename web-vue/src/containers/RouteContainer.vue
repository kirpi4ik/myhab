<template>
    <transition :duration="{ enter: 200, leave: 200 }" name="fade" mode="out-in">
        <router-view :key="$route.fullPath"></router-view>
    </transition>
</template>
<script>
    export default {
        name: 'RouteContainer',
    }
</script>
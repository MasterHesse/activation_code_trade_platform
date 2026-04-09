<script setup lang="ts">
// 初始化 auth store 以确保全局状态可用
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
// 确保 authStore 在应用启动时初始化
void authStore
</script>

<template>
  <router-view />
</template>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>

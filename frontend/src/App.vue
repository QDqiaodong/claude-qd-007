<template>
  <el-container style="min-height: 100vh">
    <el-header style="display:flex;align-items:center;gap:28px;background:#f5f7fa">
      <strong style="font-size:16px">养老院 · 房间床位与护理管理</strong>
      <el-menu mode="horizontal" :default-active="route.path" router style="flex:1;border:none;background:transparent">
        <el-menu-item index="/rooms">房间与床位</el-menu-item>
        <el-menu-item index="/residents">老人档案</el-menu-item>
        <el-menu-item index="/shifts">护理班次</el-menu-item>
        <el-menu-item index="/medicines">药品与发放</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

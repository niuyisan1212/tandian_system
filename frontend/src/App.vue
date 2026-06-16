<template>
  <a-config-provider :locale="zhCN">
    <div id="app">
      <a-layout style="min-height: 100vh">
        <!-- 顶部导航 -->
        <a-layout-header class="header">
          <div class="logo">大众点评探店路线规划系统</div>
          <a-menu
            v-model:selectedKeys="selectedKeys"
            theme="dark"
            mode="horizontal"
            :style="{ lineHeight: '64px' }"
          >
            <a-menu-item key="map" @click="$router.push('/')">
              <EnvironmentOutlined />
              地图首页
            </a-menu-item>
            <a-menu-item key="shops" @click="$router.push('/shops')">
              <ShopOutlined />
              店铺管理
            </a-menu-item>
            <a-menu-item key="lists" @click="$router.push('/lists')">
              <UnorderedListOutlined />
              探店清单
            </a-menu-item>
            <a-menu-item key="guide" @click="$router.push('/guide')">
              <BookOutlined />
              <span class="menu-text">0元探店操作手册</span>
            </a-menu-item>
          </a-menu>
        </a-layout-header>

        <!-- 内容区域 -->
        <a-layout-content style="background: #fff">
          <router-view />
        </a-layout-content>
      </a-layout>
    </div>
  </a-config-provider>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { 
  EnvironmentOutlined, 
  ShopOutlined, 
  UnorderedListOutlined,
  BookOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const selectedKeys = ref(['map'])

// 监听路由变化更新选中菜单
watch(
  () => route.path,
  (path) => {
    if (path === '/') {
      selectedKeys.value = ['map']
    } else if (path.startsWith('/shops')) {
      selectedKeys.value = ['shops']
    } else if (path.startsWith('/lists')) {
      selectedKeys.value = ['lists']
    } else if (path.startsWith('/guide')) {
      selectedKeys.value = ['guide']
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.logo {
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  margin-right: 24px;
  white-space: nowrap;
}

.menu-text {
  white-space: nowrap;
}

.header :deep(.ant-menu-horizontal) {
  flex: 1;
}

.header :deep(.ant-menu-item) {
  padding: 0 12px;
}
</style>

import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Map',
    component: () => import('@/views/MapView.vue'),
    meta: { title: '地图首页' }
  },
  {
    path: '/shops',
    name: 'Shops',
    component: () => import('@/views/ShopList.vue'),
    meta: { title: '店铺管理' }
  },
  {
    path: '/lists',
    name: 'Lists',
    component: () => import('@/views/ListManager.vue'),
    meta: { title: '探店清单' }
  },
  {
    path: '/explorers',
    name: 'Explorers',
    component: () => import('@/views/ExplorerManager.vue'),
    meta: { title: '探店员管理' }
  },
  {
    path: '/lists/:id',
    name: 'ListDetail',
    component: () => import('@/views/ListDetail.vue'),
    meta: { title: '清单详情' }
  },
  {
    path: '/guide',
    name: 'Guide',
    component: () => import('@/views/Guide.vue'),
    meta: { title: '0元探店操作手册' }
  },
  {
    path: '/tasks',
    name: 'Tasks',
    component: () => import('@/views/TaskManager.vue'),
    meta: { title: '定时任务管理' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '大众点评探店路线规划系统'
  next()
})

export default router

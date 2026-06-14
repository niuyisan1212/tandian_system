<template>
  <div class="list-detail cute-theme">
    <!-- 顶部导航栏 -->
    <div class="nav-header cute-card">
      <a-button @click="goBack" class="back-button">
        <ArrowLeftOutlined /> 返回列表
      </a-button>
      <div class="header-title">
        <span class="emoji">🗺️</span>
        <h2>{{ listData.name || '加载中...' }}</h2>
      </div>
      <div class="header-actions">
        <a-tag :color="getStatusColor(listData.status)" class="status-tag">
          {{ getStatusEmoji(listData.status) }} {{ listData.statusText }}
        </a-tag>
        <template v-if="listData.status === 0">
          <a-popconfirm
            title="标记为完成后，所有店铺将变为已探店状态，确定继续吗？"
            @confirm="handleComplete"
          >
            <a-button type="primary" class="cute-button">✅ 标记完成</a-button>
          </a-popconfirm>
        </template>
      </div>
    </div>

    <div v-if="loading" class="loading-container">
      <a-spin size="large" tip="加载中..." />
    </div>

    <div v-else class="content-area">
      <a-row :gutter="16">
        <!-- 左侧信息面板 -->
        <a-col :xs="24" :lg="10">
          <a-card title="📋 基本信息" class="info-card cute-card" :bordered="false">
            <div class="info-item">
              <CalendarOutlined class="info-icon" />
              <span><strong>任务日期：</strong>{{ listData.taskDate }}</span>
            </div>
            <div class="info-item">
              <EnvironmentOutlined class="info-icon" />
              <span><strong>起点地址：</strong>{{ listData.startAddress || '未设置' }}</span>
            </div>
          </a-card>

          <!-- 店铺列表 -->
          <a-card title="🏪 店铺列表" class="shop-card cute-card" :bordered="false">
            <a-list :data-source="listData.shops" item-layout="horizontal">
              <template #renderItem="{ item, index }">
                <a-list-item class="shop-item">
                  <a-list-item-meta>
                    <template #avatar>
                      <a-badge :count="item.visitOrder" :number-style="{ backgroundColor: '#ff69b4' }">
                        <a-avatar class="shop-avatar">{{ index + 1 }}</a-avatar>
                      </a-badge>
                    </template>
                    <template #title>
                      <span>🏪 {{ item.name }}</span>
                    </template>
                    <template #description>
                      <div class="shop-desc"><EnvironmentOutlined /> {{ item.address }}</div>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
          </a-card>
        </a-col>

        <!-- 右侧地图和路线方案 -->
        <a-col :xs="24" :lg="14">
          <!-- 地图展示 -->
          <a-card title="🗺️ 路线地图" class="map-card cute-card" :bordered="false">
            <div class="map-container">
              <AMapComponent
                container-id="detailMap"
                :center-lng="mapCenter.lng"
                :center-lat="mapCenter.lat"
                :zoom="13"
                :markers="mapMarkers"
                :route-path="currentRoutePath"
                @map-ready="onMapReady"
              />
            </div>
          </a-card>

          <!-- 路线方案 -->
          <a-card title="🚶 路线方案" class="plan-card cute-card" :bordered="false">
            <a-tabs v-model:activeKey="selectedPlan" @change="handlePlanChange">
              <a-tab-pane key="time_first" tab="⚡ 时间优先">
                <PlanCard v-if="getPlan('time_first')" :plan="getPlan('time_first')" />
                <a-empty v-else description="暂无方案数据" />
              </a-tab-pane>
              <a-tab-pane key="cost_first" tab="💰 成本优先">
                <PlanCard v-if="getPlan('cost_first')" :plan="getPlan('cost_first')" />
                <a-empty v-else description="暂无方案数据" />
              </a-tab-pane>
              <a-tab-pane key="balanced" tab="⚖️ 均衡方案">
                <PlanCard v-if="getPlan('balanced')" :plan="getPlan('balanced')" />
                <a-empty v-else description="暂无方案数据" />
              </a-tab-pane>
            </a-tabs>

            <a-button 
              type="primary" 
              block 
              class="select-plan-button"
              :disabled="listData.status !== 0"
              @click="handleSelectPlan"
            >
              ✅ 选择此方案
            </a-button>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, CalendarOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import { getListById, markListCompleted, selectPlan } from '@/api'
import AMapComponent from '@/components/AMapComponent.vue'
import PlanCard from '@/components/PlanCard.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const listData = ref({
  name: '',
  status: 0,
  statusText: '',
  taskDate: '',
  startAddress: '',
  startLat: null,
  startLng: null,
  shops: [],
  plans: [],
  selectedPlanType: null
})

const selectedPlan = ref('time_first')

// 地图中心点
const mapCenter = computed(() => {
  if (listData.value.startLng && listData.value.startLat) {
    return {
      lng: parseFloat(listData.value.startLng),
      lat: parseFloat(listData.value.startLat)
    }
  }
  if (listData.value.shops && listData.value.shops.length > 0) {
    const shop = listData.value.shops[0]
    const lng = parseFloat(shop.lng || shop.longitude)
    const lat = parseFloat(shop.lat || shop.latitude)
    if (lng && lat) {
      return { lng, lat }
    }
  }
  return { lng: 121.4737, lat: 31.2304 }
})

// 地图标记点
const mapMarkers = computed(() => {
  const markers = []
  
  // 添加起点
  if (listData.value.startLng && listData.value.startLat) {
    markers.push({
      id: 'start',
      name: '起点',
      longitude: parseFloat(listData.value.startLng),
      latitude: parseFloat(listData.value.startLat),
      order: '📍',
      active: false
    })
  }
  
  // 添加店铺
  if (listData.value.shops) {
    listData.value.shops.forEach((shop, index) => {
      const lng = parseFloat(shop.lng || shop.longitude)
      const lat = parseFloat(shop.lat || shop.latitude)
      if (lng && lat) {
        markers.push({
          id: shop.id,
          name: shop.name,
          longitude: lng,
          latitude: lat,
          order: index + 1,
          active: false
        })
      }
    })
  }
  
  return markers
})

// 当前选中方案的路线数据
const currentRoutePath = computed(() => {
  const plan = getPlan(selectedPlan.value)
  if (!plan || !plan.routeJson) return []
  
  try {
    const routeData = typeof plan.routeJson === 'string' ? JSON.parse(plan.routeJson) : plan.routeJson
    if (!routeData || !routeData.segments) return []
    
    // 从每个路段的 polyline 中提取路径坐标
    return routeData.segments.map(segment => {
      if (!segment.polyline || segment.polyline.length === 0) return null
      
      return {
        mode: segment.mode,
        path: segment.polyline.map(p => ({
          longitude: p.lng,
          latitude: p.lat
        }))
      }
    }).filter(Boolean)
  } catch (error) {
    console.error('解析路线数据失败：', error)
    return []
  }
})

const getStatusColor = (status) => {
  const colors = { 0: 'processing', 1: 'success', 2: 'default' }
  return colors[status] || 'default'
}

const getStatusEmoji = (status) => {
  const emojis = { 0: '⏳', 1: '✅', 2: '❌' }
  return emojis[status] || '❓'
}

const getPlan = (planType) => {
  return listData.value.plans.find(p => p.planType === planType)
}

const handlePlanChange = () => {
  // 切换方案时，路线会自动更新
}

const onMapReady = (map) => {
  console.log('【详情页地图】加载完成')
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await getListById(route.params.id)
    listData.value = res.data
    
    if (listData.value.selectedPlanType) {
      selectedPlan.value = listData.value.selectedPlanType
    }
  } catch (error) {
    console.error('加载详情失败：', error)
    message.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.push('/lists')
}

const handleComplete = async () => {
  try {
    await markListCompleted(route.params.id)
    message.success('标记成功！')
    loadDetail()
  } catch (error) {
    console.error('标记失败：', error)
    message.error('标记失败，请重试')
  }
}

const handleSelectPlan = async () => {
  try {
    await selectPlan(route.params.id, selectedPlan.value)
    message.success('方案选择成功！')
    loadDetail()
  } catch (error) {
    console.error('选择失败：', error)
    message.error('选择失败，请重试')
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.cute-theme {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f7 0%, #ffe8f0 50%, #ffd6e7 100%);
  padding: 20px;
}

.cute-card {
  border-radius: 16px;
  border: 2px solid #ffb6c1;
  background: white;
}

.nav-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  gap: 16px;
  flex-wrap: wrap;
}

.back-button {
  border-radius: 12px;
  border-color: #ffb6c1;
  font-weight: 500;
}

.back-button:hover {
  color: #ff69b4;
  border-color: #ff69b4;
}

.header-title {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 200px;
}

.header-title h2 {
  margin: 0;
  font-size: 22px;
  color: #ff69b4;
}

.emoji {
  font-size: 26px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.status-tag {
  border-radius: 12px;
  padding: 2px 12px;
  font-weight: 500;
}

.cute-button {
  background: linear-gradient(135deg, #ff69b4, #ff1493);
  border: none;
  border-radius: 12px;
  font-weight: 500;
}

.cute-button:hover {
  background: linear-gradient(135deg, #ff1493, #ff69b4);
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.content-area {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.info-card,
.shop-card,
.map-card,
.plan-card {
  margin-bottom: 16px;
}

.info-card :deep(.ant-card-head-title),
.shop-card :deep(.ant-card-head-title),
.map-card :deep(.ant-card-head-title),
.plan-card :deep(.ant-card-head-title) {
  color: #ff69b4;
  font-weight: 600;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #fff5f7;
  border-radius: 8px;
}

.info-icon {
  color: #ff69b4;
  font-size: 16px;
}

.shop-item {
  padding: 8px;
  border-radius: 8px;
  transition: background 0.3s;
}

.shop-item:hover {
  background: #fff5f7;
}

.shop-avatar {
  background: linear-gradient(135deg, #ff69b4, #ff1493) !important;
}

.shop-desc {
  font-size: 13px;
  color: #666;
}

.map-container {
  width: 100%;
  height: 400px;
  border-radius: 12px;
  overflow: hidden;
}

.plan-card :deep(.ant-tabs-tab) {
  color: #666;
}

.plan-card :deep(.ant-tabs-tab-active) {
  color: #ff69b4;
}

.plan-card :deep(.ant-tabs-ink-bar) {
  background: #ff69b4;
}

.select-plan-button {
  margin-top: 16px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #ff69b4, #ff1493);
  border: none;
  font-weight: 600;
  font-size: 15px;
}

.select-plan-button:hover {
  background: linear-gradient(135deg, #ff1493, #ff69b4);
}

.select-plan-button:disabled {
  background: #d9d9d9;
}

@media (max-width: 992px) {
  .nav-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-title {
    order: -1;
  }
}
</style>

<template>
  <div class="map-view">
    <div class="map-container">
      <!-- 地图容器 -->
      <div id="mapContainer" class="map"></div>
      
      <!-- 左侧面板 -->
      <div class="side-panel">
        <a-card title="探店统计" :bordered="false">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-statistic title="总店铺数" :value="statistics.totalShops" />
            </a-col>
            <a-col :span="12">
              <a-statistic title="待探店" :value="statistics.validShops" />
            </a-col>
          </a-row>
          <a-row :gutter="16" style="margin-top: 16px">
            <a-col :span="12">
              <a-statistic title="已探店" :value="statistics.visitedShops" />
            </a-col>
            <a-col :span="12">
              <a-statistic 
                title="完成率" 
                :value="statistics.completionRate" 
                suffix="%" 
              />
            </a-col>
          </a-row>
        </a-card>

        <a-card 
          title="选择清单" 
          :bordered="false" 
          style="margin-top: 16px"
        >
          <a-select
            v-model:value="selectedListId"
            placeholder="选择一个清单"
            style="width: 100%"
            @change="handleListChange"
          >
            <a-select-option :value="null">显示所有待探店店铺</a-select-option>
            <a-select-option 
              v-for="item in lists" 
              :key="item.id" 
              :value="item.id"
            >
              {{ item.name }}
            </a-select-option>
          </a-select>
          
          <a-button 
            v-if="selectedListId" 
            type="link" 
            block 
            style="margin-top: 8px"
            @click="goToDetail"
          >
            查看清单详情
          </a-button>
        </a-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getStatistics, getListPage, getValidShops, getListById } from '@/api'

const router = useRouter()

const statistics = ref({
  totalShops: 0,
  validShops: 0,
  visitedShops: 0,
  completionRate: 0
})

const lists = ref([])
const selectedListId = ref(null)
const validShops = ref([])
let map = null
let markers = []

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getStatistics()
    statistics.value = res.data
  } catch (error) {
    console.error('加载统计数据失败：', error)
  }
}

// 加载清单列表
const loadLists = async () => {
  try {
    const res = await getListPage({ pageNum: 1, pageSize: 100, status: 0 })
    lists.value = res.data.records || []
  } catch (error) {
    console.error('加载清单失败：', error)
  }
}

// 加载有效店铺
const loadValidShops = async () => {
  try {
    const res = await getValidShops()
    validShops.value = res.data || []
    renderMarkers()
  } catch (error) {
    console.error('加载店铺失败：', error)
  }
}

// 渲染地图标记
const renderMarkers = () => {
  if (!map || !validShops.value.length) return

  // 清除旧标记
  markers.forEach(marker => marker.setMap(null))
  markers = []

  // 添加新标记
  validShops.value.forEach((shop, index) => {
    const marker = new AMap.Marker({
      position: [parseFloat(shop.longitude), parseFloat(shop.latitude)],
      title: shop.name,
      content: `<div class="custom-marker">${index + 1}</div>`
    })
    
    marker.on('click', () => {
      message.info(shop.name)
    })
    
    marker.setMap(map)
    markers.push(marker)
  })

  // 自适应视野
  map.setFitView()
}

// 处理清单选择
const handleListChange = async (listId) => {
  if (!listId) {
    // 显示所有待探店店铺
    await loadValidShops()
    return
  }

  try {
    const res = await getListById(listId)
    const shops = res.data.shops || []
    
    // 清除旧标记
    markers.forEach(marker => marker.setMap(null))
    markers = []
    
    // 添加选中清单的店铺标记
    shops.forEach((shop, index) => {
      const marker = new AMap.Marker({
        position: [parseFloat(shop.longitude), parseFloat(shop.latitude)],
        title: shop.name,
        content: `<div class="custom-marker active">${index + 1}</div>`
      })
      
      marker.on('click', () => {
        message.info(shop.name)
      })
      
      marker.setMap(map)
      markers.push(marker)
    })

    // 自适应视野
    map.setFitView()
  } catch (error) {
    console.error('加载清单详情失败：', error)
  }
}

// 跳转到清单详情
const goToDetail = () => {
  if (selectedListId.value) {
    router.push(`/lists/${selectedListId.value}`)
  }
}

// 初始化地图
const initMap = () => {
  // 使用模拟地图（实际项目中应使用高德地图API）
  const container = document.getElementById('mapContainer')
  container.innerHTML = `
    <div style="
      width: 100%; 
      height: 100%; 
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 24px;
    ">
      <div style="text-align: center;">
        <div style="font-size: 48px; margin-bottom: 20px;">📍</div>
        <div>地图展示区域</div>
        <div style="font-size: 14px; margin-top: 10px; opacity: 0.8;">
          实际项目中请配置高德地图API Key
        </div>
      </div>
    </div>
  `
}

onMounted(async () => {
  await Promise.all([
    loadStatistics(),
    loadLists(),
    loadValidShops()
  ])
  initMap()
})
</script>

<style scoped>
.map-view {
  width: 100%;
  height: calc(100vh - 64px);
}

.map-container {
  position: relative;
  width: 100%;
  height: 100%;
}

.map {
  width: 100%;
  height: 100%;
}

.side-panel {
  position: absolute;
  top: 20px;
  left: 20px;
  width: 320px;
  z-index: 1000;
}

.custom-marker {
  width: 30px;
  height: 30px;
  background: #1890ff;
  border-radius: 50%;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.custom-marker.active {
  background: #52c41a;
  width: 36px;
  height: 36px;
  font-size: 16px;
}
</style>

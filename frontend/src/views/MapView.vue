<template>
  <div class="map-view">
    <div class="map-container">
      <!-- 高德地图组件 -->
      <AMapComponent
        container-id="homeMap"
        :auto-locate="true"
        :center-lng="mapCenter.lng"
        :center-lat="mapCenter.lat"
        :zoom="mapCenter.zoom"
        :markers="mapMarkers"
        @marker-click="handleMarkerClick"
        @map-ready="handleMapReady"
      />
      
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
          <a-row :gutter="16" style="margin-top: 16px">
            <a-col :span="24">
              <a-statistic title="可用人总数" :value="statistics.totalAvailableCount" suffix="人" />
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getStatistics, getListPage, getValidShops, getListById } from '@/api'
import AMapComponent from '@/components/AMapComponent.vue'

const router = useRouter()

const statistics = ref({
  totalShops: 0,
  validShops: 0,
  visitedShops: 0,
  completionRate: 0,
  totalAvailableCount: 0
})

const lists = ref([])
const selectedListId = ref(null)
const validShops = ref([])
const mapInstance = ref(null)

// 地图中心点
const mapCenter = ref({
  lng: 121.4737,
  lat: 31.2304,
  zoom: 13
})

// 地图标记点
const mapMarkers = computed(() => {
  return validShops.value.map((shop, index) => ({
    id: shop.id,
    name: shop.name,
    longitude: parseFloat(shop.longitude),
    latitude: parseFloat(shop.latitude),
    order: index + 1,
    active: false
  }))
})

// 地图准备完成
const handleMapReady = (map) => {
  mapInstance.value = map
  console.log('【地图】准备完成')
}

// 标记点点击
const handleMarkerClick = (marker) => {
  message.info(`店铺：${marker.name}`)
}

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
  } catch (error) {
    console.error('加载店铺失败：', error)
  }
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
    
    // 只显示选中清单的店铺
    validShops.value = shops.map((shop, index) => ({
      ...shop,
      active: true
    }))

    // 更新地图中心点
    if (shops.length > 0) {
      const firstShop = shops[0]
      mapCenter.value = {
        lng: parseFloat(firstShop.longitude),
        lat: parseFloat(firstShop.latitude),
        zoom: 14
      }
    }
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

onMounted(async () => {
  await Promise.all([
    loadStatistics(),
    loadLists(),
    loadValidShops()
  ])
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
</style>

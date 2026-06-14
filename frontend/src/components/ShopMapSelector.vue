<template>
  <div class="shop-map-selector">
    <!-- 地图容器 -->
    <div class="map-wrapper">
      <div ref="mapContainer" class="map"></div>
      <div class="map-legend">
        <div class="legend-item">
          <span class="legend-dot selected"></span>
          <span>已选择</span>
        </div>
        <div class="legend-item">
          <span class="legend-dot unselected"></span>
          <span>未选择</span>
        </div>
      </div>
    </div>
    
    <!-- 已选择店铺列表 -->
    <div v-if="selectedShopIds.length > 0" class="selected-shops">
      <div class="selected-header">
        <span>已选择 {{ selectedShopIds.length }} 家店铺</span>
      </div>
      <div class="selected-list">
        <a-tag 
          v-for="shop in selectedShops" 
          :key="shop.id"
          closable
          @close="toggleShop(shop)"
          class="shop-tag"
        >
          {{ shop.name }}
        </a-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'

const props = defineProps({
  shops: {
    type: Array,
    default: () => []
  },
  selectedIds: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:selectedIds'])

const mapContainer = ref(null)
const map = ref(null)
const markers = ref([])

// 选中的店铺ID列表
const selectedShopIds = computed({
  get: () => props.selectedIds,
  set: (val) => emit('update:selectedIds', val)
})

// 选中的店铺详情
const selectedShops = computed(() => {
  return props.shops.filter(shop => selectedShopIds.value.includes(shop.id))
})

// 等待高德地图SDK加载
const waitForAMap = () => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve()
      return
    }
    
    const onReady = () => {
      window.removeEventListener('amap-ready', onReady)
      resolve()
    }
    
    window.addEventListener('amap-ready', onReady)
    
    setTimeout(() => {
      window.removeEventListener('amap-ready', onReady)
      if (!window.AMap) {
        reject(new Error('SDK加载超时'))
      }
    }, 10000)
  })
}

// 初始化地图
const initMap = async () => {
  try {
    await waitForAMap()
    await nextTick()
    
    if (!mapContainer.value) {
      console.error('地图容器不存在')
      return
    }

    // 创建地图
    map.value = new AMap.Map(mapContainer.value, {
      zoom: 13,
      center: [121.4737, 31.2304],
      viewMode: '2D',
      resizeEnable: true
    })

    // 添加控件
    map.value.addControl(new AMap.Scale())
    map.value.addControl(new AMap.ToolBar())

    console.log('【ShopMapSelector】地图初始化完成')

    // 添加店铺标记
    if (props.shops && props.shops.length > 0) {
      addShopMarkers()
    }

  } catch (error) {
    console.error('地图初始化失败:', error)
    message.error('地图初始化失败: ' + error.message)
  }
}

// 添加店铺标记
const addShopMarkers = () => {
  if (!map.value || !props.shops || props.shops.length === 0) return

  // 清除旧标记
  markers.value.forEach(m => m.setMap(null))
  markers.value = []

  const points = []

  props.shops.forEach((shop, index) => {
    const lng = parseFloat(shop.longitude)
    const lat = parseFloat(shop.latitude)
    
    if (!lng || !lat) return

    points.push([lng, lat])

    const isSelected = selectedShopIds.value.includes(shop.id)
    
    // 创建自定义标记
    const markerContent = createMarkerContent(shop, isSelected, index + 1)
    
    const marker = new AMap.Marker({
      position: [lng, lat],
      content: markerContent,
      offset: new AMap.Pixel(-20, -20),
      extData: { shopId: shop.id, shop }
    })

    marker.on('click', () => {
      toggleShop(shop)
    })

    marker.setMap(map.value)
    markers.value.push(marker)
  })

  // 自适应视野
  if (points.length > 0) {
    map.value.setFitView(markers.value, false, [50, 50, 50, 50])
  }
}

// 创建标记内容
const createMarkerContent = (shop, isSelected, order) => {
  const className = isSelected ? 'shop-marker selected' : 'shop-marker'
  return `
    <div class="${className}" data-shop-id="${shop.id}">
      <div class="marker-order">${order}</div>
      <div class="marker-name">${shop.name}</div>
    </div>
  `
}

// 切换店铺选择状态
const toggleShop = (shop) => {
  const index = selectedShopIds.value.indexOf(shop.id)
  if (index > -1) {
    selectedShopIds.value = selectedShopIds.value.filter(id => id !== shop.id)
  } else {
    selectedShopIds.value = [...selectedShopIds.value, shop.id]
  }
  updateMarkers()
}

// 更新标记状态
const updateMarkers = () => {
  markers.value.forEach(marker => {
    const data = marker.getExtData()
    const isSelected = selectedShopIds.value.includes(data.shopId)
    const shop = data.shop
    const order = props.shops.findIndex(s => s.id === shop.id) + 1
    
    marker.setContent(createMarkerContent(shop, isSelected, order))
  })
}

// 监听店铺列表变化
watch(() => props.shops, () => {
  if (map.value) {
    addShopMarkers()
  }
}, { deep: true })

// 监听选择变化
watch(() => props.selectedIds, () => {
  updateMarkers()
}, { deep: true })

onMounted(() => {
  setTimeout(() => {
    initMap()
  }, 300)
})

onBeforeUnmount(() => {
  if (map.value) {
    map.value.destroy()
    map.value = null
  }
})

// 暴露方法
defineExpose({
  refresh: addShopMarkers
})
</script>

<style scoped>
.shop-map-selector {
  width: 100%;
}

.map-wrapper {
  position: relative;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.2);
  border: 2px solid #ffb6c1;
}

.map {
  width: 100%;
  height: 350px;
}

.map-legend {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.95);
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.legend-item:last-child {
  margin-bottom: 0;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.legend-dot.selected {
  background: #52c41a;
}

.legend-dot.unselected {
  background: #ff69b4;
}

.selected-shops {
  margin-top: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  border-radius: 12px;
  border: 2px solid #b7eb8f;
}

.selected-header {
  font-size: 14px;
  font-weight: 600;
  color: #52c41a;
  margin-bottom: 10px;
}

.selected-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.shop-tag {
  background: white;
  border: 1px solid #b7eb8f;
  border-radius: 16px;
  padding: 4px 12px;
  margin: 0;
}
</style>

<style>
/* 全局样式：店铺标记 */
.shop-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.shop-marker .marker-order {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #ff69b4, #ff1493);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 14px;
  border: 3px solid white;
  box-shadow: 0 3px 10px rgba(255, 105, 180, 0.4);
  transition: all 0.3s ease;
}

.shop-marker .marker-name {
  margin-top: 4px;
  padding: 2px 8px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  font-size: 11px;
  color: #333;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.shop-marker:hover {
  transform: scale(1.15);
}

.shop-marker:hover .marker-order {
  box-shadow: 0 5px 15px rgba(255, 105, 180, 0.6);
}

.shop-marker.selected .marker-order {
  background: linear-gradient(135deg, #52c41a, #389e0d);
  box-shadow: 0 3px 10px rgba(82, 196, 26, 0.5);
}

.shop-marker.selected:hover .marker-order {
  box-shadow: 0 5px 15px rgba(82, 196, 26, 0.7);
}
</style>

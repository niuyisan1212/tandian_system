<template>
  <div class="amap-container">
    <div :id="containerId" class="map"></div>
    <div v-if="!mapReady" class="map-loading">
      <a-spin tip="地图加载中..." />
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, watch, ref, nextTick } from 'vue'
import { message } from 'ant-design-vue'

const props = defineProps({
  containerId: { type: String, default: 'amapContainer' },
  centerLng: { type: Number, default: 121.4737 },
  centerLat: { type: Number, default: 31.2304 },
  zoom: { type: Number, default: 13 },
  markers: { type: Array, default: () => [] },
  routePath: { type: Array, default: () => [] },
  autoLocate: { type: Boolean, default: false }
})

const emit = defineEmits(['map-click', 'marker-click', 'map-ready'])

const map = ref(null)
const mapReady = ref(false)
const markerInstances = ref([])
const polylineInstances = ref([])

// 等待高德地图SDK加载完成
const waitForAMap = () => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve()
      return
    }
    
    if (window.AMapError) {
      reject(new Error('高德地图SDK加载失败'))
      return
    }
    
    // 监听SDK加载完成事件
    const onReady = () => {
      window.removeEventListener('amap-ready', onReady)
      resolve()
    }
    
    window.addEventListener('amap-ready', onReady)
    
    // 超时处理
    setTimeout(() => {
      window.removeEventListener('amap-ready', onReady)
      if (!window.AMap) {
        reject(new Error('高德地图SDK加载超时'))
      }
    }, 10000)
  })
}

// 初始化地图
const initMap = async () => {
  try {
    await waitForAMap()
    
    await nextTick()
    
    const container = document.getElementById(props.containerId)
    if (!container) {
      console.error('地图容器不存在:', props.containerId)
      return
    }

    // 创建地图实例
    map.value = new AMap.Map(props.containerId, {
      zoom: props.zoom,
      center: [props.centerLng, props.centerLat],
      viewMode: '2D',
      resizeEnable: true
    })

    // 添加控件
    map.value.addControl(new AMap.Scale())
    
    // 添加定位功能
    if (props.autoLocate) {
      AMap.plugin('AMap.Geolocation', () => {
        const geolocation = new AMap.Geolocation({
          enableHighAccuracy: true,
          timeout: 10000,
          showButton: true,
          showMarker: false,
          showCircle: false,
          panToLocation: true,
          zoomToAccuracy: true
        })
        map.value.addControl(geolocation)
        
        // 自动定位
        geolocation.getCurrentPosition((status, result) => {
          if (status === 'complete') {
            console.log('【地图】自动定位成功')
          } else {
            console.log('【地图】自动定位失败')
          }
        })
      })
    }
    
    // 添加工具条
    AMap.plugin('AMap.ToolBar', () => {
      map.value.addControl(new AMap.ToolBar({
        position: 'RB'
      }))
    })

    // 地图点击事件
    map.value.on('click', (e) => {
      emit('map-click', {
        lng: e.lnglat.getLng(),
        lat: e.lnglat.getLat()
      })
    })

    mapReady.value = true
    console.log('【高德地图】初始化成功')
    emit('map-ready', map.value)

    // 添加初始标记
    if (props.markers && props.markers.length > 0) {
      addMarkers()
    }
    
    // 绘制初始路线
    if (props.routePath && props.routePath.length > 0) {
      drawRoute()
    }

  } catch (error) {
    console.error('【高德地图】初始化失败：', error)
    message.error('地图初始化失败: ' + error.message)
  }
}

// 添加标记点
const addMarkers = () => {
  if (!map.value || !props.markers || props.markers.length === 0) return

  clearMarkers()

  props.markers.forEach((marker, index) => {
    const lng = parseFloat(marker.longitude)
    const lat = parseFloat(marker.latitude)
    
    if (!lng || !lat) return

    const markerInstance = new AMap.Marker({
      position: [lng, lat],
      title: marker.name,
      content: `<div class="custom-marker ${marker.active ? 'active' : ''}">${marker.order || index + 1}</div>`,
      offset: new AMap.Pixel(-15, -15)
    })

    markerInstance.on('click', () => {
      emit('marker-click', marker)
    })

    markerInstance.setMap(map.value)
    markerInstances.value.push(markerInstance)
  })

  // 自适应视野
  if (markerInstances.value.length > 0) {
    map.value.setFitView(markerInstances.value, false, [50, 50, 50, 50])
  }
}

// 绘制路线
const drawRoute = () => {
  if (!map.value || !props.routePath || props.routePath.length === 0) return

  clearRoute()

  props.routePath.forEach((route) => {
    if (!route.path || route.path.length < 2) return

    const path = route.path.map(p => [parseFloat(p.longitude), parseFloat(p.latitude)]).filter(p => p[0] && p[1])
    
    if (path.length < 2) return

    const polyline = new AMap.Polyline({
      path: path,
      strokeColor: getRouteColor(route.mode),
      strokeWeight: 6,
      strokeOpacity: 0.8,
      lineJoin: 'round',
      showDir: true
    })

    polyline.setMap(map.value)
    polylineInstances.value.push(polyline)
  })
}

const getRouteColor = (mode) => {
  const colors = {
    walking: '#52c41a',
    bus: '#1890ff',
    subway: '#f5222d'
  }
  return colors[mode] || '#1890ff'
}

const clearMarkers = () => {
  markerInstances.value.forEach(marker => marker.setMap(null))
  markerInstances.value = []
}

const clearRoute = () => {
  polylineInstances.value.forEach(polyline => polyline.setMap(null))
  polylineInstances.value = []
}

const setCenter = (lng, lat) => {
  if (map.value) {
    map.value.setCenter([lng, lat])
  }
}

const setZoom = (zoom) => {
  if (map.value) {
    map.value.setZoom(zoom)
  }
}

// 监听标记变化
watch(() => props.markers, () => {
  if (mapReady.value) {
    addMarkers()
  }
}, { deep: true })

// 监听路线变化
watch(() => props.routePath, () => {
  if (mapReady.value) {
    drawRoute()
  }
}, { deep: true })

onMounted(() => {
  setTimeout(() => {
    initMap()
  }, 100)
})

onBeforeUnmount(() => {
  if (map.value) {
    map.value.destroy()
    map.value = null
  }
})

defineExpose({
  setCenter,
  setZoom,
  addMarkers,
  drawRoute,
  clearMarkers,
  clearRoute
})
</script>

<style scoped>
.amap-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.map {
  width: 100%;
  height: 100%;
}

.map-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff5f7;
}
</style>

<style>
/* 全局样式：自定义标记 */
.custom-marker {
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, #ff69b4, #ff1493);
  border-radius: 50%;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  border: 2px solid white;
  box-shadow: 0 2px 8px rgba(255, 105, 180, 0.4);
  cursor: pointer;
  transition: all 0.3s ease;
}

.custom-marker:hover {
  transform: scale(1.2);
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.6);
}

.custom-marker.active {
  background: linear-gradient(135deg, #52c41a, #389e0d);
  width: 36px;
  height: 36px;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.6);
}
</style>

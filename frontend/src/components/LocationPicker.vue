<template>
  <div class="location-picker">
    <!-- 搜索区域 - 使用输入提示 -->
    <div class="search-section">
      <a-auto-complete
        v-model:value="searchText"
        :options="searchResults"
        :loading="searching"
        placeholder="输入地址关键词，如：天安门"
        @search="handleInputSearch"
        @select="selectSearchResult"
        allow-clear
        backfill
      >
        <template #option="item">
          <div class="autocomplete-option">
            <EnvironmentOutlined class="option-icon" />
            <div class="option-content">
              <div class="option-name">{{ item.name }}</div>
              <div class="option-address">{{ item.address }}</div>
            </div>
          </div>
        </template>
      </a-auto-complete>
      
      <a-button 
        class="locate-btn"
        :loading="locating"
        @click="getCurrentLocation"
      >
        <EnvironmentOutlined /> {{ locating ? '定位中...' : '当前位置' }}
      </a-button>
    </div>

    <!-- 地图容器 -->
    <div class="map-wrapper">
      <div ref="mapContainer" class="map"></div>
      <div class="map-tip">
        <span>📍 点击地图选择位置，或使用上方搜索</span>
      </div>
    </div>

    <!-- 已选择的位置信息 -->
    <div v-if="selectedLocation" class="selected-info">
      <div class="selected-title">
        <CheckCircleOutlined class="success-icon" />
        <strong>已选择位置</strong>
      </div>
      <div class="selected-name">{{ selectedLocation.name }}</div>
      <div class="selected-address">{{ selectedLocation.address }}</div>
      <div class="selected-coord">
        经纬度: {{ selectedLocation.longitude.toFixed(6) }}, {{ selectedLocation.latitude.toFixed(6) }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { EnvironmentOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'

const props = defineProps({
  initLng: { type: Number, default: null },
  initLat: { type: Number, default: null },
  initAddress: { type: String, default: '' }
})

const emit = defineEmits(['select'])

// 状态
const mapContainer = ref(null)
const searchText = ref('')
const searching = ref(false)
const locating = ref(false)
const searchResults = ref([])
const selectedLocation = ref(null)

// 地图相关
const map = ref(null)
const marker = ref(null)
const geocoder = ref(null)
const autoComplete = ref(null)

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
      zoom: 15,
      center: [121.4737, 31.2304],
      viewMode: '2D',
      resizeEnable: true
    })

    // 添加控件
    map.value.addControl(new AMap.Scale())
    map.value.addControl(new AMap.ToolBar())

    // 初始化地理编码服务
    AMap.plugin('AMap.Geocoder', () => {
      geocoder.value = new AMap.Geocoder({
        radius: 1000,
        extensions: 'all'
      })
      console.log('【LocationPicker】地理编码服务初始化完成')
    })

    // 初始化自动完成服务 - 输入提示
    AMap.plugin('AMap.AutoComplete', () => {
      autoComplete.value = new AMap.AutoComplete({
        city: '全国'
      })
      console.log('【LocationPicker】自动完成服务初始化完成')
    })

    // 地图点击事件
    map.value.on('click', handleMapClick)

    console.log('【LocationPicker】地图初始化完成')

    // 处理初始位置
    if (props.initLng && props.initLat) {
      setMarker(props.initLng, props.initLat, props.initAddress || '已选择的位置')
      searchText.value = props.initAddress || ''
    } else {
      // 自动定位
      getCurrentLocation()
    }

  } catch (error) {
    console.error('地图初始化失败:', error)
    message.error('地图初始化失败: ' + error.message)
  }
}

// 获取当前位置
const getCurrentLocation = () => {
  if (!map.value) return

  locating.value = true

  AMap.plugin('AMap.Geolocation', () => {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      showButton: false,
      showMarker: false,
      showCircle: false
    })

    geolocation.getCurrentPosition((status, result) => {
      locating.value = false

      if (status === 'complete') {
        const lng = result.position.getLng()
        const lat = result.position.getLat()
        const address = result.formattedAddress || '当前位置'

        setMarker(lng, lat, address)
        map.value.setCenter([lng, lat])
        map.value.setZoom(16)

        selectedLocation.value = {
          name: '当前位置',
          address: address,
          longitude: lng,
          latitude: lat
        }
        
        searchText.value = address

        emit('select', selectedLocation.value)
        message.success('定位成功')
      } else {
        console.error('定位失败:', result)
        message.warning('定位失败，请手动选择位置')
      }
    })
  })
}

// 地图点击事件
const handleMapClick = async (e) => {
  const lng = e.lnglat.getLng()
  const lat = e.lnglat.getLat()

  setMarker(lng, lat)

  // 逆地理编码获取地址
  if (geocoder.value) {
    try {
      const address = await reverseGeocode(lng, lat)
      selectedLocation.value = {
        name: '选择的位置',
        address: address,
        longitude: lng,
        latitude: lat
      }
      searchText.value = address
      emit('select', selectedLocation.value)
    } catch (error) {
      console.error('逆地理编码失败:', error)
      selectedLocation.value = {
        name: '选择的位置',
        address: `${lng.toFixed(6)}, ${lat.toFixed(6)}`,
        longitude: lng,
        latitude: lat
      }
      emit('select', selectedLocation.value)
    }
  }
}

// 设置标记
const setMarker = (lng, lat, title = '') => {
  if (!map.value) return

  // 移除旧标记
  if (marker.value) {
    marker.value.setMap(null)
  }

  // 创建新标记
  marker.value = new AMap.Marker({
    position: [lng, lat],
    draggable: true,
    animation: 'AMAP_ANIMATION_DROP'
  })

  marker.value.setMap(map.value)
  map.value.setCenter([lng, lat])

  // 标记拖拽事件
  marker.value.on('dragend', async () => {
    const pos = marker.value.getPosition()
    const newLng = pos.getLng()
    const newLat = pos.getLat()

    if (geocoder.value) {
      try {
        const address = await reverseGeocode(newLng, newLat)
        selectedLocation.value = {
          name: '选择的位置',
          address: address,
          longitude: newLng,
          latitude: newLat
        }
        emit('select', selectedLocation.value)
      } catch (error) {
        console.error('逆地理编码失败:', error)
      }
    }
  })
}

// 逆地理编码
const reverseGeocode = (lng, lat) => {
  return new Promise((resolve, reject) => {
    if (!geocoder.value) {
      reject(new Error('地理编码服务未初始化'))
      return
    }

    geocoder.value.getAddress([lng, lat], (status, result) => {
      if (status === 'complete' && result.regeocode) {
        resolve(result.regeocode.formattedAddress)
      } else {
        reject(new Error('逆地理编码失败'))
      }
    })
  })
}

// 输入提示搜索
const handleInputSearch = (value) => {
  if (!value || value.trim().length === 0) {
    searchResults.value = []
    return
  }

  if (!autoComplete.value) {
    console.error('自动完成服务未初始化')
    return
  }

  searching.value = true

  // 使用高德地图输入提示
  autoComplete.value.search(value.trim(), (status, result) => {
    searching.value = false

    if (status === 'complete' && result.tips) {
      searchResults.value = result.tips
        .filter(tip => tip.location) // 过滤掉没有坐标的结果
        .map(tip => ({
          value: tip.id,
          name: tip.name,
          address: tip.address || tip.district || '',
          longitude: tip.location.getLng(),
          latitude: tip.location.getLat()
        }))
    } else if (status === 'no_data') {
      searchResults.value = []
    } else {
      console.error('搜索失败:', status, result)
      searchResults.value = []
    }
  })
}

// 选择搜索结果
const selectSearchResult = (value, option) => {
  selectedLocation.value = option
  setMarker(option.longitude, option.latitude, option.name)
  map.value.setZoom(16)
  
  emit('select', option)
  message.success('已选择: ' + option.name)
}

// 暴露方法给父组件
defineExpose({
  setLocation: (lng, lat, address) => {
    if (map.value && lng && lat) {
      setMarker(lng, lat, address)
      selectedLocation.value = {
        name: address || '已选择的位置',
        address: address || '',
        longitude: lng,
        latitude: lat
      }
      searchText.value = address || ''
    }
  }
})

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
</script>

<style scoped>
.location-picker {
  width: 100%;
}

.search-section {
  margin-bottom: 12px;
  display: flex;
  gap: 12px;
}

.search-section :deep(.ant-select) {
  flex: 1;
}

.locate-btn {
  flex-shrink: 0;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff5f7, #ffe8f0);
  border: 2px solid #ffb6c1;
  color: #ff69b4;
  font-weight: 500;
}

.locate-btn:hover {
  background: linear-gradient(135deg, #ffe8f0, #ffd6e7);
  border-color: #ff69b4;
  color: #ff1493;
}

/* 自动完成选项样式 */
.autocomplete-option {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 0;
}

.option-icon {
  color: #ff69b4;
  font-size: 18px;
  margin-top: 2px;
}

.option-content {
  flex: 1;
  min-width: 0;
}

.option-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.option-address {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  height: 400px;
}

.map-tip {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(255, 255, 255, 0.95);
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  color: #666;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

.selected-info {
  margin-top: 12px;
  padding: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f6ffed, #d9f7be);
  border: 2px solid #b7eb8f;
}

.selected-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.success-icon {
  color: #52c41a;
  font-size: 18px;
}

.selected-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.selected-address {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.selected-coord {
  font-size: 12px;
  color: #999;
  font-family: monospace;
}
</style>

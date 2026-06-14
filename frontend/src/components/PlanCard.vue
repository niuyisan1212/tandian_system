<template>
  <div class="plan-card">
    <a-row :gutter="16">
      <a-col :span="8">
        <a-statistic title="总距离" :value="plan.totalDistanceM" suffix="米" />
      </a-col>
      <a-col :span="8">
        <a-statistic title="总耗时" :value="Math.floor(plan.totalDurationSec / 60)" suffix="分钟" />
      </a-col>
      <a-col :span="8">
        <a-statistic title="总花费" :value="plan.totalCost" prefix="¥" />
      </a-col>
    </a-row>

    <a-divider />

    <a-timeline v-if="routeData && routeData.segments">
      <a-timeline-item 
        v-for="(segment, index) in routeData.segments" 
        :key="index"
        :color="getSegmentColor(segment.mode)"
      >
        <div class="segment-header">
          <a-tag :color="getTagColor(segment.mode)" class="mode-tag">
            {{ getModeIcon(segment.mode) }} {{ getModeText(segment.mode) }}
          </a-tag>
          <span class="segment-meta">
            {{ (segment.distance_m / 1000).toFixed(1) }}公里 · {{ Math.floor(segment.duration_sec / 60) }}分钟 · ¥{{ segment.cost }}
          </span>
        </div>
        
        <!-- 显示详细换乘信息 -->
        <div v-if="segment.transitDetails && segment.transitDetails.length > 0" class="transit-details">
          <div v-for="(detail, dIndex) in segment.transitDetails" :key="dIndex" class="transit-step" :class="detail.type">
            <template v-if="detail.type === 'walking'">
              <div class="step-header">
                <span class="step-icon">🚶</span>
                <span class="step-type">步行</span>
              </div>
              <div class="step-desc">{{ detail.distance }}</div>
            </template>
            
            <template v-else-if="detail.type === 'subway'">
              <div class="step-header">
                <span class="step-icon">🚇</span>
                <span class="step-type">地铁</span>
                <span class="line-name subway">{{ detail.lineName }}</span>
                <span v-if="detail.direction" class="direction-badge">{{ detail.direction }}</span>
              </div>
              <div class="station-info">
                <div class="station-row">
                  <span class="station-dot start"></span>
                  <span class="station-label">上车</span>
                  <span class="station-name">{{ detail.departureStop }}</span>
                </div>
                <div class="station-line"></div>
                <div class="station-row">
                  <span class="station-dot end"></span>
                  <span class="station-label">下车</span>
                  <span class="station-name">{{ detail.arrivalStop }}</span>
                </div>
              </div>
              <div class="step-meta">
                <span>{{ detail.viaStops }}</span>
                <span class="price">{{ detail.price }}</span>
              </div>
            </template>
            
            <template v-else-if="detail.type === 'bus'">
              <div class="step-header">
                <span class="step-icon">🚌</span>
                <span class="step-type">公交</span>
                <span class="line-name bus">{{ detail.lineName }}</span>
                <span v-if="detail.direction" class="direction-badge">{{ detail.direction }}</span>
              </div>
              <div class="station-info">
                <div class="station-row">
                  <span class="station-dot start"></span>
                  <span class="station-label">上车</span>
                  <span class="station-name">{{ detail.departureStop }}</span>
                </div>
                <div class="station-line"></div>
                <div class="station-row">
                  <span class="station-dot end"></span>
                  <span class="station-label">下车</span>
                  <span class="station-name">{{ detail.arrivalStop }}</span>
                </div>
              </div>
              <div class="step-meta">
                <span>{{ detail.viaStops }}</span>
                <span class="price">{{ detail.price }}</span>
              </div>
            </template>
          </div>
        </div>
        
        <div v-else class="segment-instruction">{{ segment.instruction }}</div>
      </a-timeline-item>
    </a-timeline>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  plan: {
    type: Object,
    required: true
  }
})

const routeData = computed(() => {
  try {
    return typeof props.plan.routeJson === 'string' 
      ? JSON.parse(props.plan.routeJson) 
      : props.plan.routeJson
  } catch (error) {
    console.error('解析路线数据失败：', error)
    return null
  }
})

// 获取交通方式文本
const getModeText = (mode) => {
  const modes = {
    walking: '步行',
    bus: '公交',
    subway: '地铁',
    transit: '公交/地铁',
    driving: '驾车'
  }
  return modes[mode] || mode
}

// 获取交通方式图标
const getModeIcon = (mode) => {
  const icons = {
    walking: '🚶',
    bus: '🚌',
    subway: '🚇',
    transit: '🚇',
    driving: '🚗'
  }
  return icons[mode] || '🚶'
}

// 获取路段颜色
const getSegmentColor = (mode) => {
  const colors = {
    walking: 'green',
    bus: 'blue',
    subway: 'red',
    transit: 'red',
    driving: 'orange'
  }
  return colors[mode] || 'gray'
}

// 获取标签颜色
const getTagColor = (mode) => {
  const colors = {
    walking: 'success',
    bus: 'processing',
    subway: 'error',
    transit: 'error',
    driving: 'warning'
  }
  return colors[mode] || 'default'
}
</script>

<style scoped>
.plan-card {
  padding: 16px 0;
}

.segment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.mode-tag {
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 16px;
}

.segment-meta {
  color: #888;
  font-size: 13px;
}

.transit-details {
  margin: 12px 0;
  padding: 0;
}

.transit-step {
  padding: 12px;
  margin-bottom: 8px;
  background: #fafafa;
  border-radius: 12px;
  border-left: 4px solid #d9d9d9;
}

.transit-step.subway {
  border-left-color: #f5222d;
  background: linear-gradient(135deg, #fff1f0 0%, #fff 100%);
}

.transit-step.bus {
  border-left-color: #1890ff;
  background: linear-gradient(135deg, #e6f7ff 0%, #fff 100%);
}

.transit-step.walking {
  border-left-color: #52c41a;
  background: linear-gradient(135deg, #f6ffed 0%, #fff 100%);
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.step-icon {
  font-size: 20px;
}

.step-type {
  font-weight: 600;
  color: #333;
  font-size: 15px;
}

.line-name {
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.line-name.subway {
  background: #f5222d;
  color: white;
}

.line-name.bus {
  background: #1890ff;
  color: white;
}

.direction-badge {
  background: #fff7e6;
  color: #fa8c16;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.station-info {
  margin: 10px 0;
  padding-left: 28px;
}

.station-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
}

.station-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #d9d9d9;
  background: white;
  flex-shrink: 0;
}

.station-dot.start {
  border-color: #52c41a;
  background: #52c41a;
}

.station-dot.end {
  border-color: #f5222d;
  background: #f5222d;
}

.station-label {
  font-size: 12px;
  color: #888;
  width: 32px;
}

.station-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.station-line {
  width: 2px;
  height: 16px;
  background: #d9d9d9;
  margin-left: 5px;
}

.step-meta {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  padding-left: 28px;
  font-size: 13px;
  color: #666;
}

.price {
  color: #ff4d4f;
  font-weight: 600;
}

.step-desc {
  color: #666;
  font-size: 14px;
}

.segment-instruction {
  color: #666;
  font-size: 14px;
  padding: 8px 0;
}
</style>

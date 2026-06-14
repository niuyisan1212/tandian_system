<template>
  <div class="list-manager cute-theme">
    <div class="page-header">
      <h2>
        <span class="emoji">📋</span>
        探店清单
      </h2>
      <a-button type="primary" class="cute-button" @click="showModal()">
        <PlusOutlined /> 创建清单
      </a-button>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-bar cute-card">
      <a-radio-group v-model:value="filterStatus" @change="handleSearch" button-style="solid">
        <a-radio-button :value="null">🌈 全部</a-radio-button>
        <a-radio-button :value="0">⏳ 待执行</a-radio-button>
        <a-radio-button :value="1">✅ 已完成</a-radio-button>
        <a-radio-button :value="2">❌ 已取消</a-radio-button>
      </a-radio-group>
    </div>

    <!-- 清单卡片列表 -->
    <a-row :gutter="[16, 16]">
      <a-col 
        :xs="24" 
        :sm="12" 
        :md="8" 
        :lg="6" 
        v-for="item in listData" 
        :key="item.id"
      >
        <a-card hoverable class="list-card cute-card" @click="goToDetail(item.id)">
          <div class="list-header">
            <span class="list-emoji">🗺️</span>
            <h3>{{ item.name }}</h3>
          </div>
          
          <div class="list-info">
            <div class="info-item">
              <CalendarOutlined /> {{ item.taskDate }}
            </div>
            <div class="info-item">
              <EnvironmentOutlined /> {{ item.startAddress || '未设置起点' }}
            </div>
          </div>

          <div class="list-status">
            <a-tag :color="getStatusColor(item.status)" class="status-tag">
              {{ getStatusEmoji(item.status) }} {{ item.statusText }}
            </a-tag>
          </div>
          
          <div class="list-actions">
            <a-space>
              <a-button 
                size="small" 
                type="link"
                @click.stop="editList(item)"
                :disabled="item.status !== 0"
              >
                ✏️ 编辑
              </a-button>
              <a-popconfirm
                title="确定删除此清单吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(item.id)"
              >
                <a-button 
                  size="small" 
                  type="link" 
                  danger
                  @click.stop
                >
                  🗑️ 删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <a-pagination
        v-model:current="pagination.current"
        v-model:pageSize="pagination.pageSize"
        :total="pagination.total"
        show-size-changer
        @change="loadLists"
      />
    </div>

    <!-- 创建/编辑清单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingList ? '✏️ 编辑清单' : '✨ 创建清单'"
      width="1100px"
      @ok="handleSubmit"
      @cancel="handleCancel"
      class="cute-modal"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-row :gutter="24">
          <!-- 左侧表单 -->
          <a-col :span="12">
            <a-form-item label="📋 清单名称" name="name">
              <a-input v-model:value="formData.name" placeholder="请输入清单名称" />
            </a-form-item>

            <a-form-item label="📅 任务日期" name="taskDate">
              <a-date-picker
                v-model:value="formData.taskDate"
                style="width: 100%"
                placeholder="选择任务日期"
              />
            </a-form-item>

            <a-form-item label="📍 起点位置" name="startAddress">
              <a-input v-model:value="formData.startAddress" placeholder="先搜索或在地图上选择起点" />
            </a-form-item>

            <!-- 起点地图选择器 -->
            <a-form-item :wrapper-col="{ span: 22, offset: 2 }">
              <LocationPicker
                ref="locationPicker"
                :init-lng="formData.startLng"
                :init-lat="formData.startLat"
                :init-address="formData.startAddress"
                @select="handleStartLocationSelect"
              />
            </a-form-item>

            <a-form-item label="🏪 选择店铺" name="shopIds">
              <a-select
                v-model:value="formData.shopIds"
                mode="multiple"
                placeholder="请选择要探店的店铺（可在地图上点击选择）"
                style="width: 100%"
                :loading="shopLoading"
              >
                <a-select-option 
                  v-for="shop in validShops" 
                  :key="shop.id" 
                  :value="shop.id"
                >
                  🏪 {{ shop.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          
          <!-- 右侧地图选店 -->
          <a-col :span="12">
            <div class="shop-map-section">
              <div class="section-title">
                <EnvironmentOutlined /> 地图选店
                <span class="tip">点击地图标记选择/取消店铺</span>
              </div>
              <ShopMapSelector 
                :shops="validShops" 
                v-model:selected-ids="formData.shopIds"
              />
            </div>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, CalendarOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import { getListPage, createList, updateList, deleteList, getValidShops } from '@/api'
import LocationPicker from '@/components/LocationPicker.vue'
import ShopMapSelector from '@/components/ShopMapSelector.vue'
import dayjs from 'dayjs'

const router = useRouter()

const listData = ref([])
const loading = ref(false)
const filterStatus = ref(null)
const modalVisible = ref(false)
const editingList = ref(null)
const formRef = ref()
const locationPicker = ref()
const validShops = ref([])
const shopLoading = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 12,
  total: 0
})

const formData = reactive({
  name: '',
  taskDate: dayjs(),
  startAddress: '',
  startLng: 121.4737,
  startLat: 31.2304,
  shopIds: []
})

const rules = {
  name: [{ required: true, message: '请输入清单名称', trigger: 'blur' }],
  taskDate: [{ required: true, message: '请选择任务日期', trigger: 'change' }],
  shopIds: [{ required: true, message: '请选择至少一个店铺', trigger: 'change' }]
}

const getStatusColor = (status) => {
  const colors = { 0: 'processing', 1: 'success', 2: 'default' }
  return colors[status] || 'default'
}

const getStatusEmoji = (status) => {
  const emojis = { 0: '⏳', 1: '✅', 2: '❌' }
  return emojis[status] || '❓'
}

const loadLists = async () => {
  loading.value = true
  try {
    const res = await getListPage({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      status: filterStatus.value
    })
    listData.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('加载清单失败：', error)
    message.error('加载清单失败')
  } finally {
    loading.value = false
  }
}

const loadValidShops = async () => {
  shopLoading.value = true
  try {
    const res = await getValidShops()
    validShops.value = res.data || []
  } catch (error) {
    console.error('加载店铺失败：', error)
    message.error('加载店铺列表失败')
  } finally {
    shopLoading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadLists()
}

const handleStartLocationSelect = (location) => {
  formData.startAddress = location.address
  formData.startLng = location.longitude
  formData.startLat = location.latitude
}

const goToDetail = (id) => {
  router.push(`/lists/${id}`)
}

const showModal = async () => {
  editingList.value = null
  Object.assign(formData, {
    name: '',
    taskDate: dayjs(),
    startAddress: '',
    startLng: 121.4737,
    startLat: 31.2304,
    shopIds: []
  })
  await loadValidShops()
  modalVisible.value = true
}

const editList = async (item) => {
  editingList.value = item
  Object.assign(formData, {
    name: item.name,
    taskDate: dayjs(item.taskDate),
    startAddress: item.startAddress || '',
    startLng: parseFloat(item.startLng || 121.4737),
    startLat: parseFloat(item.startLat || 31.2304),
    shopIds: []
  })
  await loadValidShops()
  modalVisible.value = true
  if (locationPicker.value && formData.startLng && formData.startLat) {
    setTimeout(() => {
      locationPicker.value.setLocation(
        formData.startLng,
        formData.startLat,
        formData.startAddress
      )
    }, 200)
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    const data = {
      name: formData.name,
      taskDate: formData.taskDate.format('YYYY-MM-DD'),
      startAddress: formData.startAddress,
      startLng: formData.startLng,
      startLat: formData.startLat,
      shopIds: formData.shopIds
    }
    
    if (editingList.value) {
      await updateList(editingList.value.id, data)
      message.success('清单更新成功！')
    } else {
      await createList(data)
      message.success('清单创建成功！')
    }
    
    modalVisible.value = false
    loadLists()
  } catch (error) {
    console.error('提交失败：', error)
    message.error('操作失败，请重试')
  }
}

const handleCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

const handleDelete = async (id) => {
  try {
    await deleteList(id)
    message.success('清单删除成功！')
    loadLists()
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败，请重试')
  }
}

onMounted(() => {
  loadLists()
})
</script>

<style scoped>
.cute-theme {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f7 0%, #ffe8f0 50%, #ffd6e7 100%);
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.1);
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #ff69b4;
  display: flex;
  align-items: center;
  gap: 8px;
}

.emoji {
  font-size: 28px;
}

.cute-button {
  background: linear-gradient(135deg, #ff69b4, #ff1493);
  border: none;
  border-radius: 20px;
  height: 40px;
  font-weight: 500;
}

.cute-button:hover {
  background: linear-gradient(135deg, #ff1493, #ff69b4);
}

.cute-card {
  border-radius: 16px;
  border: 2px solid #ffb6c1;
  background: white;
  transition: all 0.3s ease;
}

.cute-card:hover {
  border-color: #ff69b4;
  box-shadow: 0 8px 24px rgba(255, 105, 180, 0.2);
  transform: translateY(-4px);
}

.filter-bar {
  padding: 16px;
  margin-bottom: 24px;
}

.list-card {
  height: 100%;
}

.list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.list-emoji {
  font-size: 24px;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.list-info {
  margin: 12px 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 13px;
  margin-bottom: 6px;
}

.list-status {
  margin: 12px 0;
}

.status-tag {
  border-radius: 12px;
  padding: 2px 12px;
}

.list-actions {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #ffb6c1;
}

.pagination-wrapper {
  margin-top: 24px;
  text-align: center;
  padding: 16px;
  background: white;
  border-radius: 16px;
}

.cute-modal :deep(.ant-modal-content) {
  border-radius: 16px;
}

.cute-modal :deep(.ant-modal-header) {
  border-radius: 16px 16px 0 0;
  background: linear-gradient(135deg, #fff5f7, #ffe8f0);
}

.shop-map-section {
  height: 100%;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #ff69b4;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title .tip {
  font-size: 12px;
  font-weight: 400;
  color: #888;
}
</style>

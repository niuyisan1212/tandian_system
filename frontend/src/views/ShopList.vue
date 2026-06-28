<template>
  <div class="shop-list cute-theme">
    <div class="page-header">
      <h2>
        <span class="emoji">🏪</span>
        店铺管理
      </h2>
      <a-space>
        <a-button @click="handleDownloadTemplate">
          <DownloadOutlined /> 下载模板
        </a-button>
        <a-upload
          :show-upload-list="false"
          :before-upload="handleImport"
          accept=".xlsx,.xls"
        >
          <a-button>
            <UploadOutlined /> 批量导入
          </a-button>
        </a-upload>
        <a-button @click="handleExport">
          <ExportOutlined /> 批量导出
        </a-button>
        <a-button type="primary" class="cute-button" @click="showModal()">
          <PlusOutlined /> 新增店铺
        </a-button>
      </a-space>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-bar cute-card">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="🔍 搜索店铺名称或地址"
        style="width: 300px"
        @search="handleSearch"
      />
      <a-select
        v-model:value="filterCategory"
        placeholder="店铺类别"
        style="width: 150px; margin-left: 16px"
        allowClear
        @change="handleSearch"
      >
        <a-select-option value="美食">🍜 美食</a-select-option>
        <a-select-option value="娱乐">🎮 娱乐</a-select-option>
        <a-select-option value="购物">🛍️ 购物</a-select-option>
        <a-select-option value="服务">💆 服务</a-select-option>
        <a-select-option value="其他">📌 其他</a-select-option>
      </a-select>
      <a-select
        v-model:value="filterStatus"
        placeholder="探店状态"
        style="width: 150px; margin-left: 16px"
        allowClear
        @change="handleSearch"
      >
        <a-select-option :value="0">
          <span>⏳ 未探店</span>
        </a-select-option>
        <a-select-option :value="1">
          <span>✅ 已探店</span>
        </a-select-option>
      </a-select>
      <a-select
        v-model:value="filterIsValid"
        placeholder="生效状态"
        style="width: 150px; margin-left: 16px"
        allowClear
        @change="handleSearch"
      >
        <a-select-option :value="1">
          <span>🟢 生效中</span>
        </a-select-option>
        <a-select-option :value="0">
          <span>⚫ 已失效</span>
        </a-select-option>
      </a-select>
    </div>

    <!-- 店铺卡片列表 -->
    <div class="shop-cards">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="shop in shopList" :key="shop.id">
          <a-card hoverable class="shop-card cute-card" @click="showModal(shop)">
            <template #cover>
              <div class="shop-avatar">
                <span class="emoji-large">🏪</span>
              </div>
            </template>
            <a-card-meta :title="shop.name">
              <template #description>
                <div class="shop-info">
                  <div class="info-item">
                    <EnvironmentOutlined /> {{ shop.address }}
                  </div>
                  <div class="info-item">
                    <PhoneOutlined /> {{ shop.phone || '未填写' }}
                  </div>
                  <div class="status-tags">
                    <a-tag :color="getCategoryColor(shop.category)">
                      {{ getCategoryEmoji(shop.category) }} {{ shop.categoryText || '其他' }}
                    </a-tag>
                    <a-tag :color="shop.visitStatus === 0 ? 'processing' : 'success'">
                      {{ shop.visitStatus === 0 ? '⏳ 未探店' : '✅ 已探店' }}
                    </a-tag>
                    <a-tag :color="shop.isValid ? 'success' : 'default'">
                      {{ shop.isValid ? '🟢 有效' : '⚫ 无效' }}
                    </a-tag>
                  </div>
                </div>
              </template>
            </a-card-meta>
            <template #actions>
              <a-space>
                <a-button size="small" type="link" @click.stop="showModal(shop)">
                  ✏️ 编辑
                </a-button>
                <a-popconfirm
                  title="确定删除此店铺吗？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="handleDelete(shop.id)"
                >
                  <a-button size="small" type="link" danger @click.stop>
                    🗑️ 删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <a-pagination
        v-model:current="pagination.current"
        v-model:pageSize="pagination.pageSize"
        :total="pagination.total"
        show-size-changer
        @change="loadShops"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingShop ? '✏️ 编辑店铺' : '✨ 新增店铺'"
      width="900px"
      @ok="handleSubmit"
      @cancel="handleCancel"
      class="cute-modal"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="🏪 店铺名称" name="name">
          <a-input-search
            v-model:value="formData.name"
            placeholder="输入店铺名称后点击搜索，自动填充信息"
            :loading="autoFilling"
            @search="handleAutoFill"
            enter-button="搜索填充"
          />
        </a-form-item>

        <a-form-item label="📁 店铺类别" name="category">
          <a-select v-model:value="formData.category" placeholder="请选择店铺类别">
            <a-select-option value="美食">🍜 美食</a-select-option>
            <a-select-option value="娱乐">🎮 娱乐</a-select-option>
            <a-select-option value="购物">🛍️ 购物</a-select-option>
            <a-select-option value="服务">💆 服务</a-select-option>
            <a-select-option value="其他">📌 其他</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="📍 店铺位置" name="address">
          <a-input v-model:value="formData.address" placeholder="先搜索或在地图上选择位置" />
        </a-form-item>

        <!-- 地图选择器 -->
        <a-form-item :wrapper-col="{ span: 20, offset: 2 }">
          <LocationPicker
            ref="locationPicker"
            :init-lng="formData.longitude"
            :init-lat="formData.latitude"
            :init-address="formData.address"
            @select="handleLocationSelect"
          />
        </a-form-item>

        <a-form-item label="📞 联系电话" name="phone">
          <a-input v-model:value="formData.phone" placeholder="请输入联系电话" />
        </a-form-item>

        <a-form-item label="🕐 营业时间" name="businessHours">
          <a-input v-model:value="formData.businessHours" placeholder="请输入营业时间" />
        </a-form-item>

        <a-form-item label="📝 备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            placeholder="请输入备注"
            :rows="3"
          />
        </a-form-item>

        <a-form-item label="⏰ 过期时间" name="expireTime">
          <a-date-picker
            v-model:value="formData.expireTime"
            style="width: 100%"
            placeholder="选择过期时间（可选）"
          />
        </a-form-item>

        <a-form-item label="🎯 探店状态" name="visitStatus">
          <a-radio-group v-model:value="formData.visitStatus">
            <a-radio :value="0">⏳ 未探店</a-radio>
            <a-radio :value="1">✅ 已探店</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, EnvironmentOutlined, PhoneOutlined, UploadOutlined, ExportOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { getShopList, createShop, updateShop, deleteShop, exportShops, importShops, downloadTemplate, searchShopInfo } from '@/api'
import LocationPicker from '@/components/LocationPicker.vue'
import dayjs from 'dayjs'

const shopList = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const filterCategory = ref(undefined)
const filterStatus = ref(undefined)
const filterIsValid = ref(1)
const modalVisible = ref(false)
const editingShop = ref(null)
const formRef = ref()
const locationPicker = ref()
const autoFilling = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 12,
  total: 0
})

const formData = reactive({
  name: '',
  category: '美食',
  address: '',
  longitude: 121.4737,
  latitude: 31.2304,
  phone: '',
  businessHours: '',
  remark: '',
  visitStatus: 0,
  expireTime: null
})

const rules = {
  name: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入或选择店铺地址', trigger: 'blur' }]
}

// 加载店铺列表
const loadShops = async () => {
  loading.value = true
  try {
    const res = await getShopList({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchKeyword.value,
      category: filterCategory.value,
      visitStatus: filterStatus.value,
      isValid: filterIsValid.value
    })
    
    shopList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('加载店铺列表失败：', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadShops()
}

// 地图位置选择
const handleLocationSelect = (location) => {
  formData.address = location.address
  formData.longitude = location.longitude
  formData.latitude = location.latitude
}

// 根据店名自动填充信息
const handleAutoFill = async () => {
  if (!formData.name || formData.name.trim() === '') {
    message.warning('请先输入店铺名称')
    return
  }
  
  autoFilling.value = true
  try {
    const res = await searchShopInfo(formData.name.trim())
    if (res.data) {
      const info = res.data
      console.log(info)
      // 填充地址
      formData.address = info.address

      
      // 填充经纬度
      if (info.longitude && info.latitude) {
        formData.longitude = info.longitude
        formData.latitude = info.latitude
        
        // 同步更新地图位置
        if (locationPicker.value) {
          setTimeout(() => {
            locationPicker.value.setLocation(
              info.longitude,
              info.latitude,
              info.address || formData.address
            )
          }, 200)
        }
      }
      
      // 填充电话
      formData.phone = info.phone
      
      // 填充营业时间
      formData.businessHours = info.business_hour

      
      message.success('已自动填充店铺信息')
    } else {
      message.info('未找到该店铺信息，请手动填写')
    }
  } catch (error) {
    console.error('搜索店铺信息失败：', error)
    message.error('搜索失败，请手动填写')
  } finally {
    autoFilling.value = false
  }
}

// 显示弹窗
const showModal = (shop = null) => {
  editingShop.value = shop
  
  if (shop) {
    Object.assign(formData, {
      name: shop.name,
      category: shop.category || '美食',
      address: shop.address,
      longitude: parseFloat(shop.longitude),
      latitude: parseFloat(shop.latitude),
      phone: shop.phone || '',
      businessHours: shop.businessHours || '',
      remark: shop.remark || '',
      visitStatus: shop.visitStatus,
      expireTime: shop.expireTime ? dayjs(shop.expireTime) : null
    })
  } else {
    Object.assign(formData, {
      name: '',
      category: '美食',
      address: '',
      longitude: 121.4737,
      latitude: 31.2304,
      phone: '',
      businessHours: '',
      remark: '',
      visitStatus: 0,
      expireTime: null
    })
  }
  
  modalVisible.value = true
  
  // 设置地图位置
  if (locationPicker.value && formData.longitude && formData.latitude) {
    setTimeout(() => {
      locationPicker.value.setLocation(
        formData.longitude,
        formData.latitude,
        formData.address
      )
    }, 200)
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    const data = {
      ...formData,
      expireTime: formData.expireTime ? formData.expireTime.format('YYYY-MM-DD') : null
    }
    
    if (editingShop.value) {
      await updateShop(editingShop.value.id, data)
      message.success('✨ 更新成功！')
    } else {
      await createShop(data)
      message.success('🎉 创建成功！')
    }
    
    modalVisible.value = false
    loadShops()
  } catch (error) {
    console.error('提交失败：', error)
  }
}

// 取消
const handleCancel = () => {
  modalVisible.value = false
  formRef.value.resetFields()
}

// 删除店铺
const handleDelete = async (id) => {
  try {
    await deleteShop(id)
    message.success('🗑️ 删除成功！')
    loadShops()
  } catch (error) {
    console.error('删除失败：', error)
  }
}

// 导出店铺列表
const handleExport = async () => {
  try {
    message.loading({ content: '正在导出...', key: 'export' })
    const res = await exportShops({
      keyword: searchKeyword.value,
      visitStatus: filterStatus.value,
      isValid: filterIsValid.value
    })
    
    // 创建下载链接
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `店铺列表_${dayjs().format('YYYY-MM-DD')}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    
    message.success({ content: '导出成功！', key: 'export' })
  } catch (error) {
    console.error('导出失败：', error)
    message.error({ content: '导出失败', key: 'export' })
  }
}

// 导入店铺列表
const handleImport = async (file) => {
  try {
    message.loading({ content: '正在导入...', key: 'import' })
    const res = await importShops(file)
    message.success({ content: res.message || '导入成功！', key: 'import', duration: 5 })
    loadShops()
  } catch (error) {
    console.error('导入失败：', error)
    message.error({ content: '导入失败', key: 'import' })
  }
  return false // 阻止默认上传行为
}

// 下载导入模板
const handleDownloadTemplate = async () => {
  try {
    message.loading({ content: '正在下载模板...', key: 'template' })
    const res = await downloadTemplate()
    
    // 创建下载链接
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '店铺导入模板.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
    
    message.success({ content: '模板下载成功！', key: 'template' })
  } catch (error) {
    console.error('下载模板失败：', error)
    message.error({ content: '下载模板失败', key: 'template' })
  }
}

// 获取类别颜色
const getCategoryColor = (category) => {
  const colors = {
    '美食': 'magenta',
    '娱乐': 'purple',
    '购物': 'blue',
    '服务': 'cyan',
    '其他': 'default'
  }
  return colors[category] || 'default'
}

// 获取类别emoji
const getCategoryEmoji = (category) => {
  const emojis = {
    '美食': '🍜',
    '娱乐': '🎮',
    '购物': '🛍️',
    '服务': '💆',
    '其他': '📌'
  }
  return emojis[category] || '📌'
}

onMounted(() => {
  loadShops()
})
</script>

<style scoped>
.shop-list {
  padding: 24px;
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f6 100%);
  min-height: calc(100vh - 64px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  background: linear-gradient(120deg, #ff6b9d, #ff8fab);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  display: flex;
  align-items: center;
  gap: 12px;
}

.emoji {
  font-size: 32px;
}

.cute-button {
  background: linear-gradient(135deg, #ff6b9d, #ff8fab);
  border: none;
  border-radius: 20px;
  font-weight: bold;
  transition: all 0.3s ease;
}

.cute-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(255, 107, 157, 0.3);
}

.cute-card {
  border-radius: 16px;
  border: 2px solid #ffe0e8;
  transition: all 0.3s ease;
}

.cute-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(255, 107, 157, 0.15);
}

.filter-bar {
  padding: 20px;
  background: white;
  border-radius: 16px;
  margin-bottom: 24px;
}

.shop-cards {
  margin-bottom: 24px;
}

.shop-card {
  height: 100%;
}

.shop-avatar {
  height: 120px;
  background: linear-gradient(135deg, #fff0f6 0%, #ffe0e8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji-large {
  font-size: 48px;
}

.shop-info {
  padding: 8px 0;
}

.info-item {
  margin: 4px 0;
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-tags {
  margin-top: 8px;
}

.pagination-wrapper {
  text-align: center;
  margin-top: 24px;
}

.cute-modal :deep(.ant-modal-content) {
  border-radius: 20px;
}

.cute-modal :deep(.ant-modal-header) {
  border-radius: 20px 20px 0 0;
  background: linear-gradient(135deg, #fff0f6, #ffe0e8);
}

.cute-modal :deep(.ant-modal-title) {
  font-weight: bold;
  color: #ff6b9d;
}
</style>

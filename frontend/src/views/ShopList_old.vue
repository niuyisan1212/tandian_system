<template>
  <div class="shop-list">
    <div class="page-header">
      <h2>店铺管理</h2>
      <a-button type="primary" @click="showModal()">
        <PlusOutlined /> 新增店铺
      </a-button>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-bar">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索店铺名称或地址"
        style="width: 300px"
        @search="handleSearch"
      />
      <a-select
        v-model:value="filterStatus"
        placeholder="探店状态"
        style="width: 150px; margin-left: 16px"
        allowClear
        @change="handleSearch"
      >
        <a-select-option :value="0">未探店</a-select-option>
        <a-select-option :value="1">已探店</a-select-option>
      </a-select>
    </div>

    <!-- 店铺列表 -->
    <a-table
      :columns="columns"
      :data-source="shopList"
      :loading="loading"
      :pagination="pagination"
      @change="handleTableChange"
      :scroll="{ x: 1200 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <a @click="showDetail(record)">{{ record.name }}</a>
        </template>
        
        <template v-if="column.key === 'address'">
          <span style="color: #666">{{ record.address }}</span>
        </template>

        <template v-if="column.key === 'visitStatus'">
          <a-tag :color="record.visitStatus === 0 ? 'blue' : 'green'">
            {{ record.visitStatus === 0 ? '未探店' : '已探店' }}
          </a-tag>
        </template>

        <template v-if="column.key === 'isValid'">
          <a-tag :color="record.isValid ? 'success' : 'default'">
            {{ record.isValid ? '有效' : '无效' }}
          </a-tag>
        </template>

        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="showModal(record)">编辑</a>
            <a-divider type="vertical" />
            <a-popconfirm
              title="确定删除此店铺吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(record.id)"
            >
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingShop ? '编辑店铺' : '新增店铺'"
      width="600px"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="店铺名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入店铺名称" />
        </a-form-item>

        <a-form-item label="店铺地址" name="address">
          <a-input v-model:value="formData.address" placeholder="请输入店铺地址" />
        </a-form-item>

        <a-form-item label="经度" name="longitude">
          <a-input-number
            v-model:value="formData.longitude"
            :precision="7"
            :min="-180"
            :max="180"
            style="width: 100%"
            placeholder="请输入经度"
          />
        </a-form-item>

        <a-form-item label="纬度" name="latitude">
          <a-input-number
            v-model:value="formData.latitude"
            :precision="7"
            :min="-90"
            :max="90"
            style="width: 100%"
            placeholder="请输入纬度"
          />
        </a-form-item>

        <a-form-item label="联系电话" name="phone">
          <a-input v-model:value="formData.phone" placeholder="请输入联系电话" />
        </a-form-item>

        <a-form-item label="营业时间" name="businessHours">
          <a-input v-model:value="formData.businessHours" placeholder="请输入营业时间" />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            placeholder="请输入备注"
            :rows="3"
          />
        </a-form-item>

        <a-form-item label="过期时间" name="expireTime">
          <a-date-picker
            v-model:value="formData.expireTime"
            show-time
            style="width: 100%"
            placeholder="选择过期时间（可选）"
          />
        </a-form-item>

        <a-form-item label="探店状态" name="visitStatus">
          <a-radio-group v-model:value="formData.visitStatus">
            <a-radio :value="0">未探店</a-radio>
            <a-radio :value="1">已探店</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getShopList, createShop, updateShop, deleteShop } from '@/api'
import dayjs from 'dayjs'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '店铺名称', dataIndex: 'name', key: 'name', width: 150 },
  { title: '地址', dataIndex: 'address', key: 'address', width: 250 },
  { title: '联系电话', dataIndex: 'phone', key: 'phone', width: 120 },
  { title: '营业时间', dataIndex: 'businessHours', key: 'businessHours', width: 120 },
  { title: '状态', dataIndex: 'visitStatus', key: 'visitStatus', width: 100 },
  { title: '有效性', dataIndex: 'isValid', key: 'isValid', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 150 },
  { title: '操作', key: 'action', fixed: 'right', width: 150 }
]

const shopList = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const filterStatus = ref(undefined)
const modalVisible = ref(false)
const editingShop = ref(null)
const formRef = ref()

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`
})

const formData = reactive({
  name: '',
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
  address: [{ required: true, message: '请输入店铺地址', trigger: 'blur' }],
  longitude: [{ required: true, message: '请输入经度', trigger: 'blur' }],
  latitude: [{ required: true, message: '请输入纬度', trigger: 'blur' }]
}

// 加载店铺列表
const loadShops = async () => {
  loading.value = true
  try {
    const res = await getShopList({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: searchKeyword.value,
      visitStatus: filterStatus.value
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

// 分页变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadShops()
}

// 显示弹窗
const showModal = (shop = null) => {
  editingShop.value = shop
  
  if (shop) {
    Object.assign(formData, {
      name: shop.name,
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
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    const data = {
      ...formData,
      expireTime: formData.expireTime ? formData.expireTime.format('YYYY-MM-DD HH:mm:ss') : null
    }
    
    if (editingShop.value) {
      await updateShop(editingShop.value.id, data)
      message.success('更新成功')
    } else {
      await createShop(data)
      message.success('创建成功')
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
    message.success('删除成功')
    loadShops()
  } catch (error) {
    console.error('删除失败：', error)
  }
}

// 显示详情
const showDetail = (record) => {
  message.info(`店铺详情：${record.name}`)
}

onMounted(() => {
  loadShops()
})
</script>

<style scoped>
.shop-list {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
}

.filter-bar {
  margin-bottom: 16px;
}
</style>

<template>
  <div class="list-manager">
    <div class="page-header">
      <h2>探店清单</h2>
      <a-button type="primary" @click="showModal()">
        <PlusOutlined /> 创建清单
      </a-button>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-bar">
      <a-radio-group v-model:value="filterStatus" @change="handleSearch">
        <a-radio-button :value="null">全部</a-radio-button>
        <a-radio-button :value="0">待执行</a-radio-button>
        <a-radio-button :value="1">已完成</a-radio-button>
        <a-radio-button :value="2">已取消</a-radio-button>
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
        <a-card hoverable @click="goToDetail(item.id)">
          <template #title>
            <div class="card-title">
              {{ item.name }}
            </div>
          </template>
          
          <template #extra>
            <a-tag :color="getStatusColor(item.status)">
              {{ item.statusText }}
            </a-tag>
          </template>

          <p class="card-date">{{ item.taskDate }}</p>
          <p class="card-address">{{ item.startAddress || '未设置起点' }}</p>
          
          <div class="card-footer">
            <a-space>
              <a-button 
                size="small" 
                @click.stop="editList(item)"
                :disabled="item.status !== 0"
              >
                编辑
              </a-button>
              <a-popconfirm
                title="确定删除此清单吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(item.id)"
              >
                <a-button 
                  size="small" 
                  danger 
                  @click.stop
                >
                  删除
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
      :title="editingList ? '编辑清单' : '创建清单'"
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
        <a-form-item label="清单名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入清单名称" />
        </a-form-item>

        <a-form-item label="任务日期" name="taskDate">
          <a-date-picker
            v-model:value="formData.taskDate"
            style="width: 100%"
            placeholder="选择任务日期"
          />
        </a-form-item>

        <a-form-item label="起点地址" name="startAddress">
          <a-input v-model:value="formData.startAddress" placeholder="请输入起点地址" />
        </a-form-item>

        <a-form-item label="起点经度" name="startLng">
          <a-input-number
            v-model:value="formData.startLng"
            :precision="7"
            :min="-180"
            :max="180"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="起点纬度" name="startLat">
          <a-input-number
            v-model:value="formData.startLat"
            :precision="7"
            :min="-90"
            :max="90"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="选择店铺" name="shopIds">
          <a-select
            v-model:value="formData.shopIds"
            mode="multiple"
            placeholder="请选择店铺"
            style="width: 100%"
            :loading="shopLoading"
          >
            <a-select-option 
              v-for="shop in validShops" 
              :key="shop.id" 
              :value="shop.id"
            >
              {{ shop.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { getListPage, createList, updateList, deleteList, getValidShops } from '@/api'
import dayjs from 'dayjs'

const router = useRouter()

const listData = ref([])
const loading = ref(false)
const filterStatus = ref(null)
const modalVisible = ref(false)
const editingList = ref(null)
const formRef = ref()
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

// 获取状态颜色
const getStatusColor = (status) => {
  const colors = {
    0: 'blue',
    1: 'green',
    2: 'default'
  }
  return colors[status] || 'default'
}

// 加载清单列表
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
  } finally {
    loading.value = false
  }
}

// 加载有效店铺
const loadValidShops = async () => {
  shopLoading.value = true
  try {
    const res = await getValidShops()
    validShops.value = res.data || []
  } catch (error) {
    console.error('加载店铺失败：', error)
  } finally {
    shopLoading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadLists()
}

// 跳转详情
const goToDetail = (id) => {
  router.push(`/lists/${id}`)
}

// 显示弹窗
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

// 编辑清单
const editList = async (item) => {
  editingList.value = item
  
  // TODO: 从详情接口获取完整数据
  
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
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    const data = {
      ...formData,
      taskDate: formData.taskDate.format('YYYY-MM-DD')
    }
    
    if (editingList.value) {
      await updateList(editingList.value.id, data)
      message.success('更新成功')
    } else {
      await createList(data)
      message.success('创建成功')
    }
    
    modalVisible.value = false
    loadLists()
  } catch (error) {
    console.error('提交失败：', error)
  }
}

// 取消
const handleCancel = () => {
  modalVisible.value = false
  formRef.value.resetFields()
}

// 删除清单
const handleDelete = async (id) => {
  try {
    await deleteList(id)
    message.success('删除成功')
    loadLists()
  } catch (error) {
    console.error('删除失败：', error)
  }
}

onMounted(() => {
  loadLists()
})
</script>

<style scoped>
.list-manager {
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
  margin-bottom: 24px;
}

.card-title {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-date {
  color: #666;
  margin: 8px 0;
}

.card-address {
  color: #999;
  font-size: 13px;
  margin: 0 0 12px;
}

.card-footer {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
  margin-top: 12px;
}

.pagination-wrapper {
  margin-top: 24px;
  text-align: center;
}
</style>

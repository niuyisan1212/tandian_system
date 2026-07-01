<template>
  <div class="explorer-manager cute-theme">
    <div class="page-header">
      <h2>
        <span class="emoji">🧑‍🍳</span>
        探店员管理
      </h2>
      <a-space>
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="🔍 搜索姓名或手机号"
          style="width: 250px"
          @search="loadExplorers"
          allowClear
        />
        <a-button type="primary" class="cute-button" @click="showModal()">
          <PlusOutlined /> 新增探店员
        </a-button>
      </a-space>
    </div>

    <!-- 探店员卡片列表 -->
    <div class="explorer-cards">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="explorer in explorerList" :key="explorer.id">
          <a-card hoverable class="explorer-card cute-card">
            <div class="explorer-avatar">
              <span class="emoji-large">🧑‍🍳</span>
            </div>
            <a-card-meta :title="explorer.name">
              <template #description>
                <div class="explorer-info">
                  <div class="info-item">
                    <PhoneOutlined /> {{ explorer.phone || '未填写' }}
                  </div>
                  <div class="info-item">
                    <MailOutlined /> {{ explorer.email || '未填写' }}
                  </div>
                  <div class="info-item" v-if="explorer.remark">
                    <FileTextOutlined /> {{ explorer.remark }}
                  </div>
                </div>
              </template>
            </a-card-meta>
            <template #actions>
              <a-button size="small" type="link" @click="showModal(explorer)">
                ✏️ 编辑
              </a-button>
              <a-popconfirm
                title="确定删除此探店员吗？关联的店铺将解除关联。"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(explorer.id)"
              >
                <a-button size="small" type="link" danger>
                  🗑️ 删除
                </a-button>
              </a-popconfirm>
            </template>
          </a-card>
        </a-col>
      </a-row>

      <a-empty v-if="explorerList.length === 0" description="暂无探店员，点击新增添加" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingExplorer ? '✏️ 编辑探店员' : '✨ 新增探店员'"
      width="500px"
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
        <a-form-item label="🧑‍🍳 姓名" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入探店员姓名" />
        </a-form-item>

        <a-form-item label="📞 手机号" name="phone">
          <a-input v-model:value="formData.phone" placeholder="请输入手机号" />
        </a-form-item>

        <a-form-item label="📧 邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入邮箱（可选）" />
        </a-form-item>

        <a-form-item label="📝 备注" name="remark">
          <a-textarea
            v-model:value="formData.remark"
            placeholder="请输入备注（可选）"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, PhoneOutlined, MailOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import { getExplorerList, createExplorer, updateExplorer, deleteExplorer } from '@/api'

const explorerList = ref([])
const loading = ref(false)
const searchKeyword = ref('')
const modalVisible = ref(false)
const editingExplorer = ref(null)
const formRef = ref()

const formData = reactive({
  name: '',
  phone: '',
  email: '',
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入探店员姓名', trigger: 'blur' }]
}

// 加载探店员列表
const loadExplorers = async () => {
  loading.value = true
  try {
    const res = await getExplorerList({ keyword: searchKeyword.value })
    explorerList.value = res.data || []
  } catch (error) {
    console.error('加载探店员列表失败：', error)
  } finally {
    loading.value = false
  }
}

// 显示弹窗
const showModal = (explorer = null) => {
  editingExplorer.value = explorer

  if (explorer) {
    Object.assign(formData, {
      name: explorer.name,
      phone: explorer.phone || '',
      email: explorer.email || '',
      remark: explorer.remark || ''
    })
  } else {
    Object.assign(formData, {
      name: '',
      phone: '',
      email: '',
      remark: ''
    })
  }

  modalVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()

    const data = { ...formData }

    if (editingExplorer.value) {
      await updateExplorer(editingExplorer.value.id, data)
      message.success('✨ 更新成功！')
    } else {
      await createExplorer(data)
      message.success('🎉 创建成功！')
    }

    modalVisible.value = false
    loadExplorers()
  } catch (error) {
    console.error('提交失败：', error)
  }
}

// 取消
const handleCancel = () => {
  modalVisible.value = false
  formRef.value.resetFields()
}

// 删除探店员
const handleDelete = async (id) => {
  try {
    await deleteExplorer(id)
    message.success('🗑️ 删除成功！')
    loadExplorers()
  } catch (error) {
    console.error('删除失败：', error)
  }
}

onMounted(() => {
  loadExplorers()
})
</script>

<style scoped>
.explorer-manager {
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

.explorer-cards {
  margin-bottom: 24px;
}

.explorer-card {
  height: 100%;
  text-align: center;
}

.explorer-avatar {
  height: 100px;
  background: linear-gradient(135deg, #fff0f6 0%, #ffe0e8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  width: 100px;
  margin: 16px auto 12px;
}

.emoji-large {
  font-size: 48px;
}

.explorer-info {
  padding: 8px 0;
  text-align: left;
}

.info-item {
  margin: 4px 0;
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
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

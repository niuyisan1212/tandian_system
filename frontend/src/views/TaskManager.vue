<template>
  <div class="task-manager cute-theme">
    <div class="page-header">
      <h2>
        <span class="emoji">⏰</span>
        定时任务管理
      </h2>
    </div>

    <!-- 邮件服务器配置 -->
    <a-card class="cute-card" style="margin-bottom: 24px">
      <template #title>
        <span><MailOutlined /> 邮件服务器配置（全局）</span>
      </template>
      <template #extra>
        <a-button type="primary" size="small" @click="handleSaveSmtp">
          保存配置
        </a-button>
      </template>
      <a-form layout="inline" style="flex-wrap: wrap; gap: 12px">
        <a-form-item label="SMTP服务器">
          <a-input v-model:value="smtpConfig.smtp_host" style="width: 180px" placeholder="smtp.163.com" />
        </a-form-item>
        <a-form-item label="端口">
          <a-input-number v-model:value="smtpConfig.smtp_port" style="width: 100px" :min="1" :max="65535" />
        </a-form-item>
        <a-form-item label="SSL">
          <a-switch v-model:checked="smtpConfig.smtp_ssl" checked-value="true" un-checked-value="false" />
        </a-form-item>
        <a-form-item label="发件邮箱">
          <a-input v-model:value="smtpConfig.smtp_username" style="width: 220px" placeholder="xxx@163.com" />
        </a-form-item>
        <a-form-item label="授权码">
          <a-input-password v-model:value="smtpConfig.smtp_password" style="width: 200px" placeholder="SMTP授权码" />
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 定时任务列表 -->
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :sm="24" :md="12" v-for="task in taskList" :key="task.id">
        <a-card class="task-card cute-card">
          <template #title>
            <div class="task-title">
              <span>{{ task.taskName }}</span>
              <a-tag :color="task.enabled === 1 ? 'success' : 'default'">
                {{ task.enabled === 1 ? '运行中' : '已停止' }}
              </a-tag>
            </div>
          </template>
          <template #extra>
            <a-switch
              :checked="task.enabled === 1"
              @change="handleToggle(task)"
              checked-children="开"
              un-checked-children="关"
            />
          </template>

          <p class="task-desc">{{ task.taskDesc }}</p>

          <div class="task-info">
            <div class="info-row">
              <ClockCircleOutlined />
              <span>执行频率：</span>
              <code>{{ task.cronExpression }}</code>
              <span class="cron-text">（{{ formatCron(task.cronExpression) }}）</span>
            </div>
            <div class="info-row">
              <MailOutlined />
              <span>邮件通知：</span>
              <a-tag :color="task.notifyEnabled === 1 ? 'blue' : 'default'" size="small">
                {{ task.notifyEnabled === 1 ? '已开启' : '未开启' }}
              </a-tag>
            </div>
            <div class="info-row" v-if="task.emails">
              <span class="email-list">{{ task.emails }}</span>
            </div>
            <div class="info-row" v-if="task.lastExecuteTime">
              <HistoryOutlined />
              <span>上次执行：</span>
              <span>{{ task.lastExecuteTime }}</span>
              <a-tag :color="task.lastExecuteStatus === 'success' ? 'success' : 'error'" size="small" style="margin-left: 8px">
                {{ task.lastExecuteStatus === 'success' ? '成功' : '失败' }}
              </a-tag>
            </div>
            <div class="info-row" v-if="task.lastExecuteMessage">
              <span class="exec-message">{{ task.lastExecuteMessage }}</span>
            </div>
          </div>

          <div class="task-actions">
            <a-button type="primary" size="small" @click="handleTrigger(task)" :disabled="task.enabled !== 1">
              <ThunderboltOutlined /> 手动触发
            </a-button>
            <a-button size="small" @click="showEditModal(task)">
              <EditOutlined /> 编辑
            </a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="editModalVisible"
      :title="'编辑 - ' + editingTask?.taskName"
      width="550px"
      @ok="handleEditSubmit"
      @cancel="editModalVisible = false"
      class="cute-modal"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="任务名称">
          <a-input v-model:value="editForm.taskName" disabled />
        </a-form-item>

        <a-form-item label="Cron表达式">
          <a-input v-model:value="editForm.cronExpression" placeholder="如：0 0 14 * * ?" />
          <div class="cron-preview">{{ formatCron(editForm.cronExpression) }}</div>
        </a-form-item>

        <a-form-item label="邮件通知">
          <a-switch v-model:checked="editForm.notifyEnabled" :checked-value="1" :un-checked-value="0" />
        </a-form-item>

        <a-form-item label="通知邮箱" v-if="editForm.notifyEnabled === 1">
          <a-select
            v-model:value="editForm.emailList"
            mode="tags"
            placeholder="输入邮箱后按回车添加"
            style="width: 100%"
          />
          <div class="form-tip">输入邮箱后按回车添加，支持多个邮箱</div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ClockCircleOutlined, MailOutlined, HistoryOutlined, ThunderboltOutlined, EditOutlined } from '@ant-design/icons-vue'
import { getScheduledTasks, updateScheduledTask, toggleScheduledTask, triggerScheduledTask, getSystemConfig, updateSystemConfig } from '@/api'

const taskList = ref([])
const editModalVisible = ref(false)
const editingTask = ref(null)

const smtpConfig = reactive({
  smtp_host: 'smtp.163.com',
  smtp_port: 465,
  smtp_ssl: 'true',
  smtp_username: '',
  smtp_password: ''
})

const editForm = reactive({
  id: null,
  taskName: '',
  cronExpression: '',
  notifyEnabled: 0,
  emailList: []
})

const loadTasks = async () => {
  try {
    const res = await getScheduledTasks()
    taskList.value = res.data || []
  } catch (error) {
    console.error('加载定时任务失败：', error)
  }
}

const loadSmtpConfig = async () => {
  try {
    const res = await getSystemConfig()
    const configs = res.data || []
    configs.forEach(c => {
      if (c.configKey === 'smtp_host') smtpConfig.smtp_host = c.configValue || 'smtp.163.com'
      if (c.configKey === 'smtp_port') smtpConfig.smtp_port = c.configValue ? parseInt(c.configValue) : 465
      if (c.configKey === 'smtp_ssl') smtpConfig.smtp_ssl = c.configValue || 'true'
      if (c.configKey === 'smtp_username') smtpConfig.smtp_username = c.configValue || ''
      if (c.configKey === 'smtp_password') smtpConfig.smtp_password = c.configValue || ''
    })
  } catch (error) {
    console.error('加载邮件配置失败：', error)
  }
}

const handleSaveSmtp = async () => {
  try {
    await updateSystemConfig({
      smtp_host: smtpConfig.smtp_host,
      smtp_port: String(smtpConfig.smtp_port),
      smtp_ssl: smtpConfig.smtp_ssl,
      smtp_username: smtpConfig.smtp_username,
      smtp_password: smtpConfig.smtp_password
    })
    message.success('邮件配置保存成功')
  } catch (error) {
    console.error('保存邮件配置失败：', error)
    message.error('保存失败')
  }
}

const handleToggle = async (task) => {
  try {
    const res = await toggleScheduledTask(task.id)
    message.success(res.message || '操作成功')
    loadTasks()
  } catch (error) {
    console.error('切换任务状态失败：', error)
    message.error('操作失败')
  }
}

const handleTrigger = async (task) => {
  try {
    message.loading({ content: '正在执行...', key: 'trigger' })
    const res = await triggerScheduledTask(task.id)
    message.success({ content: res.message || '触发成功', key: 'trigger', duration: 3 })
    loadTasks()
  } catch (error) {
    console.error('手动触发失败：', error)
    message.error({ content: '触发失败', key: 'trigger' })
  }
}

const showEditModal = (task) => {
  editingTask.value = task
  Object.assign(editForm, {
    id: task.id,
    taskName: task.taskName,
    cronExpression: task.cronExpression,
    notifyEnabled: task.notifyEnabled || 0,
    emailList: task.emails ? task.emails.split(',').filter(e => e.trim()) : []
  })
  editModalVisible.value = true
}

const handleEditSubmit = async () => {
  try {
    const data = {
      taskName: editForm.taskName,
      cronExpression: editForm.cronExpression,
      notifyEnabled: editForm.notifyEnabled,
      emails: editForm.emailList.join(',')
    }
    const res = await updateScheduledTask(editForm.id, data)
    message.success(res.message || '更新成功')
    editModalVisible.value = false
    loadTasks()
  } catch (error) {
    console.error('更新定时任务失败：', error)
    message.error('更新失败')
  }
}

const formatCron = (cron) => {
  if (!cron) return ''
  const map = {
    '0 0 14 * * ?': '每天 14:00',
    '0 10 14 ? * MON': '每周一 14:10',
    '0 0 14 ? * MON-FRI': '工作日 14:00',
    '0 0 10,14 * * ?': '每天 10:00 和 14:00',
  }
  return map[cron] || cron
}

onMounted(() => {
  loadTasks()
  loadSmtpConfig()
})
</script>

<style scoped>
.task-manager {
  padding: 24px;
  background: linear-gradient(135deg, #fff5f7 0%, #fff0f6 100%);
  min-height: calc(100vh - 64px);
}

.page-header {
  margin-bottom: 24px;
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

.cute-card {
  border-radius: 16px;
  border: 2px solid #ffe0e8;
  transition: all 0.3s ease;
}

.cute-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(255, 107, 157, 0.15);
}

.task-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-desc {
  color: #666;
  font-size: 13px;
  margin-bottom: 16px;
}

.task-info {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 8px 0;
  font-size: 13px;
  color: #555;
  flex-wrap: wrap;
}

.email-list {
  color: #1890ff;
  font-size: 12px;
  word-break: break-all;
}

.exec-message {
  font-size: 12px;
  color: #999;
  word-break: break-all;
}

.cron-text {
  color: #999;
  font-size: 12px;
}

.task-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.cron-preview {
  color: #ff6b9d;
  font-size: 12px;
  margin-top: 4px;
}

.form-tip {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
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

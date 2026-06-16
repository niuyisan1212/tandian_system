<template>
  <div class="guide-page">
    <div class="guide-container">
      <!-- 页面标题 -->
      <div class="guide-header">
        <h1>大众点评 0 元探店操作手册</h1>
        <p class="subtitle">从报名到履约，完整操作指引</p>
      </div>

      <!-- 时间节点 -->
      <div class="section">
        <div class="section-title">
          <span class="section-icon">⏰</span>
          <span>关键时间节点</span>
        </div>
        <div class="card-row">
          <div class="time-card">
            <div class="time-label">完成当周报名</div>
            <div class="time-value">每周三之前</div>
          </div>
          <div class="time-card">
            <div class="time-label">抽奖开放</div>
            <div class="time-value">每周四</div>
          </div>
        </div>
      </div>

      <!-- 抽奖条件 -->
      <div class="section">
        <div class="section-title">
          <span class="section-icon">🎯</span>
          <span>抽奖条件</span>
        </div>
        <div class="condition-card">
          <p>报名抽奖前需满足：</p>
          <ul>
            <li>写 <strong>2 篇</strong>优质笔记</li>
            <li>每篇：<strong>3 张图 + 50 字以上</strong></li>
            <li>必须带话题 <a-tag color="magenta">#0元玩转这座城</a-tag></li>
          </ul>
        </div>
      </div>

      <!-- 写笔记步骤 -->
      <div class="section">
        <div class="section-title">
          <span class="section-icon">📝</span>
          <span>写笔记步骤</span>
        </div>

        <div class="step-card">
          <div class="step-number">1</div>
          <div class="step-content">
            <div class="step-text">打开大众点评 → 我的 → 0元探店</div>
            <div class="step-image">
              <img src="/guide/step1.png" alt="步骤1" @click="previewImage('/guide/step1.png')" />
            </div>
          </div>
        </div>

        <div class="step-card">
          <div class="step-number">2</div>
          <div class="step-content">
            <div class="step-text">点击"写笔记解锁"，写两篇符合要求的笔记即可</div>
            <div class="step-image">
              <img src="/guide/step2.png" alt="步骤2" @click="previewImage('/guide/step2.png')" />
            </div>
          </div>
        </div>
      </div>

      <!-- 抽奖步骤 -->
      <div class="section">
        <div class="section-title">
          <span class="section-icon">🎁</span>
          <span>抽奖步骤</span>
        </div>

        <div class="step-card">
          <div class="step-number">1</div>
          <div class="step-content">
            <div class="step-text">打开 0 元探店页面，查看"本期奖池"</div>
            <div class="step-image">
              <img src="/guide/step3.png" alt="步骤3" @click="previewImage('/guide/step3.png')" />
            </div>
          </div>
        </div>

        <div class="step-card">
          <div class="step-number">2</div>
          <div class="step-content">
            <div class="step-text">点击"更多筛选" → 选择"我可报名"，即可看到当期可报名的门店</div>
            <div class="step-image">
              <img src="/guide/step4.png" alt="步骤4" @click="previewImage('/guide/step4.png')" />
            </div>
          </div>
        </div>
      </div>

      <!-- 履约要求 -->
      <div class="section">
        <div class="section-title">
          <span class="section-icon">✅</span>
          <span>履约笔记要求</span>
        </div>
        <div class="require-table">
          <div class="table-row table-header">
            <div class="table-cell label-cell">要求</div>
            <div class="table-cell value-cell">说明</div>
          </div>
          <div class="table-row">
            <div class="table-cell label-cell">完成时限</div>
            <div class="table-cell value-cell">体验后 <strong>7 天内</strong>发布并通过审核</div>
          </div>
          <div class="table-row">
            <div class="table-cell label-cell">关联门店</div>
            <div class="table-cell value-cell">笔记必须关联门店</div>
          </div>
          <div class="table-row">
            <div class="table-cell label-cell">话题标签</div>
            <div class="table-cell value-cell">必须带 <a-tag color="magenta">#0元玩转这座城</a-tag></div>
          </div>
          <div class="table-row">
            <div class="table-cell label-cell">图文笔记</div>
            <div class="table-cell value-cell">字数 ≥ <strong>100 字</strong>，图片 ≥ <strong>6 张</strong></div>
          </div>
          <div class="table-row">
            <div class="table-cell label-cell">视频笔记</div>
            <div class="table-cell value-cell">时长 ≥ <strong>30 秒</strong></div>
          </div>
        </div>
      </div>

      <!-- 提示 -->
      <div class="section">
        <div class="tip-card tip-info">
          <span class="tip-icon">💡</span>
          <span>建议中奖后第一时间拍照记录，避免临近截止日期匆忙完成导致审核不通过。</span>
        </div>
        <div class="tip-card tip-warn">
          <span class="tip-icon">⚠️</span>
          <span>中奖后如果无法在截止时间去体验，一定要手动取消，否则 <strong>1 个月内将不能报名</strong>。</span>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <a-modal
      v-model:open="previewVisible"
      :footer="null"
      :width="800"
      class="preview-modal"
    >
      <img :src="previewSrc" style="width: 100%" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const previewVisible = ref(false)
const previewSrc = ref('')

const previewImage = (src) => {
  previewSrc.value = src
  previewVisible.value = true
}
</script>

<style scoped>
.guide-page {
  min-height: calc(100vh - 64px);
  background: linear-gradient(135deg, #fff5f7 0%, #ffe8f0 50%, #ffd6e7 100%);
  padding: 32px 16px;
}

.guide-container {
  max-width: 720px;
  margin: 0 auto;
}

/* 页面标题 */
.guide-header {
  text-align: center;
  margin-bottom: 40px;
}

.guide-header h1 {
  font-size: 28px;
  color: #333;
  margin: 0 0 8px 0;
}

.subtitle {
  color: #888;
  font-size: 15px;
  margin: 0;
}

/* 章节 */
.section {
  margin-bottom: 36px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.section-icon {
  font-size: 24px;
}

/* 时间节点卡片 */
.card-row {
  display: flex;
  gap: 16px;
}

.time-card {
  flex: 1;
  background: white;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  border: 2px solid #ffb6c1;
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.1);
}

.time-label {
  font-size: 14px;
  color: #888;
  margin-bottom: 8px;
}

.time-value {
  font-size: 20px;
  font-weight: 700;
  color: #ff69b4;
}

/* 条件卡片 */
.condition-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  border: 2px solid #ffb6c1;
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.1);
}

.condition-card p {
  color: #666;
  margin-bottom: 12px;
}

.condition-card ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.condition-card li {
  padding: 8px 0;
  font-size: 15px;
  color: #333;
  border-bottom: 1px dashed #ffe0eb;
}

.condition-card li:last-child {
  border-bottom: none;
}

.condition-card li::before {
  content: '✓ ';
  color: #ff69b4;
  font-weight: bold;
}

/* 步骤卡片 */
.step-card {
  display: flex;
  gap: 16px;
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 12px;
  border: 2px solid #ffb6c1;
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.1);
}

.step-number {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #ff69b4, #ff1493);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-text {
  font-size: 15px;
  color: #333;
  margin-bottom: 12px;
  line-height: 1.6;
}

.step-image {
  text-align: center;
}

.step-image img {
  max-width: 280px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  transition: transform 0.2s;
}

.step-image img:hover {
  transform: scale(1.02);
}

/* 要求表格 */
.require-table {
  background: white;
  border-radius: 16px;
  overflow: hidden;
  border: 2px solid #ffb6c1;
  box-shadow: 0 4px 12px rgba(255, 105, 180, 0.1);
}

.table-row {
  display: flex;
  border-bottom: 1px dashed #ffe0eb;
}

.table-row:last-child {
  border-bottom: none;
}

.table-header {
  background: linear-gradient(135deg, #fff5f7, #ffe8f0);
}

.table-cell {
  padding: 12px 16px;
  font-size: 14px;
}

.label-cell {
  width: 100px;
  flex-shrink: 0;
  color: #888;
  display: flex;
  align-items: center;
}

.table-header .label-cell {
  color: #333;
  font-weight: 600;
}

.value-cell {
  flex: 1;
  color: #333;
}

.table-header .value-cell {
  font-weight: 600;
}

/* 提示卡片 */
.tip-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 20px;
  border-radius: 12px;
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 1.6;
}

.tip-info {
  background: #e6f7ff;
  border: 1px solid #91d5ff;
  color: #0050b3;
}

.tip-warn {
  background: #fff7e6;
  border: 1px solid #ffd591;
  color: #ad6800;
}

.tip-icon {
  font-size: 20px;
  flex-shrink: 0;
}

/* 移动端适配 */
@media (max-width: 576px) {
  .guide-page {
    padding: 20px 12px;
  }

  .guide-header h1 {
    font-size: 22px;
  }

  .card-row {
    flex-direction: column;
  }

  .step-image img {
    max-width: 240px;
  }

  .label-cell {
    width: 80px;
  }
}
</style>

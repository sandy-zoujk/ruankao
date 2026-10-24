<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { chapterApi } from '../api'

const chapters = ref([]), loading = ref(false), visible = ref(false), current = reactive({})
async function load() { loading.value = true; try { chapters.value = (await chapterApi.list()).data } finally { loading.value = false } }
function edit(row) { Object.assign(current, row); visible.value = true }
async function save() { await chapterApi.update(current.id, current); ElMessage.success('掌握度已更新'); visible.value = false; load() }
const priorityText = value => ['', '低', '较低', '中', '高', '核心'][value]
onMounted(load)
</script>

<template>
  <div>
    <div class="page-actions"><div><strong>优先突破高频、高分、薄弱章节</strong><p>掌握度结合看书、刷题正确率和能否独立复述综合判断。</p></div></div>
    <el-table v-loading="loading" :data="chapters" class="panel" stripe>
      <el-table-column prop="name" label="章节" min-width="210" />
      <el-table-column prop="category" label="科目" width="120"><template #default="{ row }"><el-tag effect="plain">{{ row.category }}</el-tag></template></el-table-column>
      <el-table-column prop="priority" label="重要度" width="120"><template #default="{ row }"><el-rate :model-value="row.priority" disabled /></template></el-table-column>
      <el-table-column label="掌握度" min-width="220"><template #default="{ row }"><el-progress :percentage="row.mastery" :color="row.mastery >= 80 ? '#2f887c' : row.mastery >= 50 ? '#d8a944' : '#ce6b52'" /></template></el-table-column>
      <el-table-column label="刷题正确率" width="130"><template #default="{ row }">{{ row.questionCount ? Math.round(row.correctCount * 100 / row.questionCount) : 0 }}%（{{ row.correctCount }}/{{ row.questionCount }}）</template></el-table-column>
      <el-table-column prop="lastReviewDate" label="最近复习" width="120"><template #default="{ row }">{{ row.lastReviewDate || '尚未复习' }}</template></el-table-column>
      <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="edit(row)">更新进度</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="visible" title="更新章节进度" width="500px">
      <el-form label-position="top" class="dialog-form">
        <el-form-item label="章节"><el-input :model-value="current.name" disabled /></el-form-item>
        <el-form-item :label="`重要度：${priorityText(current.priority)}`"><el-rate v-model="current.priority" /></el-form-item>
        <el-form-item :label="`掌握度：${current.mastery}%`"><el-slider v-model="current.mastery" show-input /></el-form-item>
        <el-row :gutter="16"><el-col :span="12"><el-form-item label="累计做题"><el-input-number v-model="current.questionCount" :min="0" /></el-form-item></el-col><el-col :span="12"><el-form-item label="答对题数"><el-input-number v-model="current.correctCount" :min="0" :max="current.questionCount" /></el-form-item></el-col></el-row>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>


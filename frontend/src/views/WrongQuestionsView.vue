<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chapterApi, wrongQuestionApi } from '../api'

const questions = ref([]), chapters = ref([]), loading = ref(false), visible = ref(false), filter = ref('all'), formRef = ref()
const form = reactive({ title: '', content: '', correctAnswer: '', myAnswer: '', errorReason: '知识点不熟', chapterId: null })
const shown = computed(() => filter.value === 'all' ? questions.value : questions.value.filter(q => filter.value === 'mastered' ? q.mastered : !q.mastered))
async function load() { loading.value = true; try { const [q, c] = await Promise.all([wrongQuestionApi.list(), chapterApi.list()]); questions.value = q.data; chapters.value = c.data } finally { loading.value = false } }
function openCreate() { Object.assign(form, { title: '', content: '', correctAnswer: '', myAnswer: '', errorReason: '知识点不熟', chapterId: null }); visible.value = true }
async function save() { await formRef.value.validate(); const { chapterId, ...payload } = form; await wrongQuestionApi.create(payload, chapterId); ElMessage.success('错题已加入复盘清单'); visible.value = false; load() }
async function review(row, mastered) { await wrongQuestionApi.review(row.id, mastered); ElMessage.success(mastered ? '已标记掌握' : '已安排 3 天后再复习'); load() }
async function remove(row) { await ElMessageBox.confirm(`确定删除“${row.title}”吗？`, '删除错题', { type: 'warning' }); await wrongQuestionApi.remove(row.id); ElMessage.success('已删除'); load() }
onMounted(load)
</script>

<template>
  <div>
    <div class="page-actions"><div><strong>每道错题都要找到真正的错误原因</strong><p>重点记录判断路径、遗漏条件以及下次如何避免。</p></div><div><el-radio-group v-model="filter" style="margin-right:12px"><el-radio-button value="all">全部</el-radio-button><el-radio-button value="pending">待掌握</el-radio-button><el-radio-button value="mastered">已掌握</el-radio-button></el-radio-group><el-button type="primary" @click="openCreate">录入错题</el-button></div></div>
    <el-table v-loading="loading" :data="shown" class="panel" stripe>
      <el-table-column prop="title" label="题目" min-width="220" />
      <el-table-column label="章节" width="160"><template #default="{ row }">{{ row.chapter?.name || '未分类' }}</template></el-table-column>
      <el-table-column prop="errorReason" label="错误原因" width="130"><template #default="{ row }"><el-tag type="warning" effect="plain">{{ row.errorReason }}</el-tag></template></el-table-column>
      <el-table-column prop="reviewCount" label="复习次数" width="100" />
      <el-table-column prop="nextReviewDate" label="下次复习" width="120"><template #default="{ row }">{{ row.nextReviewDate || '—' }}</template></el-table-column>
      <el-table-column label="状态" width="95"><template #default="{ row }"><el-tag :type="row.mastered ? 'success' : 'danger'">{{ row.mastered ? '已掌握' : '待掌握' }}</el-tag></template></el-table-column>
      <el-table-column type="expand"><template #default="{ row }"><div style="padding:8px 30px"><p class="question-body"><strong>题目：</strong>{{ row.content }}</p><p class="question-body"><strong>我的答案：</strong>{{ row.myAnswer || '未记录' }}</p><p class="question-body"><strong>正确答案：</strong>{{ row.correctAnswer || '未记录' }}</p></div></template></el-table-column>
      <el-table-column label="操作" width="210" fixed="right"><template #default="{ row }"><el-button link type="success" @click="review(row, true)">已掌握</el-button><el-button link type="primary" @click="review(row, false)">再复习</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="visible" title="录入错题" width="620px">
      <el-form ref="formRef" :model="form" :rules="{ title:[{required:true,message:'请输入标题'}],content:[{required:true,message:'请输入题目内容'}],errorReason:[{required:true,message:'请选择错误原因'}] }" label-position="top">
        <el-form-item label="简短标题" prop="title"><el-input v-model="form.title" placeholder="例如：关键路径总时差计算错误" /></el-form-item>
        <el-row :gutter="16"><el-col :span="12"><el-form-item label="所属章节"><el-select v-model="form.chapterId" filterable clearable style="width:100%"><el-option v-for="c in chapters" :key="c.id" :label="c.name" :value="c.id" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="错误原因" prop="errorReason"><el-select v-model="form.errorReason" style="width:100%"><el-option v-for="x in ['知识点不熟','审题错误','计算错误','记忆混淆','时间不足']" :key="x" :label="x" :value="x" /></el-select></el-form-item></el-col></el-row>
        <el-form-item label="题目内容" prop="content"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="我的答案"><el-input v-model="form.myAnswer" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="正确答案与解析"><el-input v-model="form.correctAnswer" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>


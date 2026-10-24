<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { planApi } from '../api'

const plans = ref([]), loading = ref(false), visible = ref(false), editingId = ref(null)
const formRef = ref()
const emptyForm = () => ({ title: '', category: '项目管理', planDate: new Date().toISOString().slice(0, 10), durationMinutes: 60, completed: false, notes: '' })
const form = reactive(emptyForm())
const rules = { title: [{ required: true, message: '请输入学习内容', trigger: 'blur' }], category: [{ required: true, message: '请选择分类', trigger: 'change' }], planDate: [{ required: true, message: '请选择日期', trigger: 'change' }] }

async function load() { loading.value = true; try { plans.value = (await planApi.list()).data } finally { loading.value = false } }
function openCreate() { editingId.value = null; Object.assign(form, emptyForm()); visible.value = true }
function openEdit(row) { editingId.value = row.id; Object.assign(form, row); visible.value = true }
async function save() {
  await formRef.value.validate()
  editingId.value ? await planApi.update(editingId.value, form) : await planApi.create(form)
  ElMessage.success(editingId.value ? '计划已更新' : '计划已创建'); visible.value = false; load()
}
async function toggle(row) { await planApi.toggle(row.id); load() }
async function remove(row) { await ElMessageBox.confirm(`确定删除“${row.title}”吗？`, '删除计划', { type: 'warning' }); await planApi.remove(row.id); ElMessage.success('已删除'); load() }
onMounted(load)
</script>

<template>
  <div>
    <div class="page-actions"><div><strong>把大目标拆成今天能完成的一步</strong><p>建议工作日 60–90 分钟，周末安排成套练习与论文。</p></div><el-button type="primary" @click="openCreate">新增计划</el-button></div>
    <el-table v-loading="loading" :data="plans" class="panel" stripe>
      <el-table-column label="状态" width="78"><template #default="{ row }"><el-checkbox :model-value="row.completed" @change="toggle(row)" /></template></el-table-column>
      <el-table-column prop="planDate" label="日期" width="120" sortable />
      <el-table-column prop="title" label="学习内容" min-width="220"><template #default="{ row }"><span :style="row.completed ? 'text-decoration:line-through;color:#9aa5a1' : ''">{{ row.title }}</span></template></el-table-column>
      <el-table-column prop="category" label="分类" width="130"><template #default="{ row }"><el-tag effect="plain">{{ row.category }}</el-tag></template></el-table-column>
      <el-table-column prop="durationMinutes" label="时长" width="100"><template #default="{ row }">{{ row.durationMinutes }} 分钟</template></el-table-column>
      <el-table-column prop="notes" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="visible" :title="editingId ? '编辑学习计划' : '新增学习计划'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="dialog-form">
        <el-form-item label="学习内容" prop="title"><el-input v-model="form.title" placeholder="例如：复习挣值管理并完成 20 道题" /></el-form-item>
        <el-row :gutter="16"><el-col :span="12"><el-form-item label="分类" prop="category"><el-select v-model="form.category" style="width:100%"><el-option v-for="x in ['综合知识','项目管理','案例分析','论文','刷题']" :key="x" :label="x" :value="x" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="日期" prop="planDate"><el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col></el-row>
        <el-form-item label="预计时长（分钟）"><el-input-number v-model="form.durationMinutes" :min="15" :max="480" :step="15" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.notes" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>


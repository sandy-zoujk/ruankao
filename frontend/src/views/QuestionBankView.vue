<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { questionBankApi } from "../api";

const items = ref([]),
  total = ref(0),
  loading = ref(false),
  visible = ref(false),
  formRef = ref(),
  uploadRef = ref();
const query = reactive({
  keyword: "",
  chapter: "",
  enabled: null,
  page: 1,
  size: 20,
});
const empty = () => ({
  id: null,
  questionCode: "",
  chapter: "",
  content: "",
  optionA: "",
  optionB: "",
  optionC: "",
  optionD: "",
  correctOption: "A",
  explanation: "",
  enabled: true,
});
const form = reactive(empty());
async function load() {
  loading.value = true;
  try {
    const r = await questionBankApi.list(query);
    items.value = r.data.items;
    total.value = r.data.total;
  } finally {
    loading.value = false;
  }
}
function open(row) {
  Object.assign(form, empty(), row || {});
  visible.value = true;
}
async function save() {
  await formRef.value.validate();
  if (form.id) await questionBankApi.update(form.id, form);
  else await questionBankApi.create(form);
  ElMessage.success("题目已保存");
  visible.value = false;
  load();
}
async function toggle(row) {
  await questionBankApi.setEnabled(row.id, !row.enabled);
  ElMessage.success("状态已更新");
  load();
}
async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除“${row.questionCode}”吗？已有练习记录时将改为停用。`,
    "删除题目",
    { type: "warning" },
  );
  await questionBankApi.remove(row.id);
  ElMessage.success("操作成功");
  load();
}
async function download() {
  const r = await questionBankApi.template();
  const url = URL.createObjectURL(r);
  const a = document.createElement("a");
  a.href = url;
  a.download = "question-bank-template.xlsx";
  a.click();
  URL.revokeObjectURL(url);
}
async function importFile(file) {
  loading.value = true;
  try {
    const r = await questionBankApi.importExcel(file.raw);
    const d = r.data;
    ElMessage.success(
      `导入完成：新增 ${d.inserted}，更新 ${d.updated}，失败 ${d.failed}`,
    );
    if (d.errors?.length)
      await ElMessageBox.alert(d.errors.slice(0, 20).join("\n"), "导入错误", {
        customClass: "import-errors",
      });
    await load();
  } finally {
    loading.value = false;
    uploadRef.value?.clearFiles();
  }
  return false;
}
onMounted(load);
</script>

<template>
  <div class="page-actions">
    <div>
      <strong>公共选择题题库</strong>
      <p>按唯一题号维护，停用后的题目不会进入新练习。</p>
    </div>
    <div class="question-actions">
      <el-button @click="download">下载模板</el-button
      ><el-upload
        ref="uploadRef"
        :auto-upload="false"
        :show-file-list="false"
        accept=".xlsx"
        :on-change="importFile"
        ><el-button>导入 Excel</el-button></el-upload
      ><el-button type="primary" @click="open()">新增题目</el-button>
    </div>
  </div>
  <section class="panel">
    <div class="filter-row">
      <el-input
        v-model="query.keyword"
        clearable
        placeholder="题号或题干"
        @keyup.enter="
          query.page = 1;
          load();
        "
      /><el-input
        v-model="query.chapter"
        clearable
        placeholder="章节"
        @keyup.enter="
          query.page = 1;
          load();
        "
      /><el-select v-model="query.enabled" clearable placeholder="全部状态"
        ><el-option label="启用" :value="true" /><el-option
          label="停用"
          :value="false" /></el-select
      ><el-button
        type="primary"
        @click="
          query.page = 1;
          load();
        "
        >查询</el-button
      >
    </div>
    <el-table v-loading="loading" :data="items"
      ><el-table-column
        prop="questionCode"
        label="题号"
        width="120"
      /><el-table-column
        prop="chapter"
        label="章节"
        width="170"
      /><el-table-column
        prop="content"
        label="题干"
        min-width="260"
        show-overflow-tooltip
      /><el-table-column
        prop="correctOption"
        label="答案"
        width="70"
      /><el-table-column label="状态" width="80"
        ><template #default="{ row }"
          ><el-tag :type="row.enabled ? 'success' : 'info'">{{
            row.enabled ? "启用" : "停用"
          }}</el-tag></template
        ></el-table-column
      ><el-table-column label="操作" width="210"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="open(row)">编辑</el-button
          ><el-button link @click="toggle(row)">{{
            row.enabled ? "停用" : "启用"
          }}</el-button
          ><el-button link type="danger" @click="remove(row)"
            >删除</el-button
          ></template
        ></el-table-column
      ></el-table
    >
    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="load"
    />
  </section>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑题目' : '新增题目'"
    width="760px"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="{
        questionCode: [{ required: true, message: '请输入题目编号' }],
        chapter: [{ required: true, message: '请输入章节' }],
        content: [{ required: true, message: '请输入题干' }],
      }"
      label-position="top"
    >
      <el-row :gutter="16"
        ><el-col :span="8"
          ><el-form-item label="题目编号" prop="questionCode"
            ><el-input v-model="form.questionCode" /></el-form-item></el-col
        ><el-col :span="12"
          ><el-form-item label="章节" prop="chapter"
            ><el-input v-model="form.chapter" /></el-form-item></el-col
        ><el-col :span="4"
          ><el-form-item label="启用"
            ><el-switch v-model="form.enabled" /></el-form-item></el-col
      ></el-row>
      <el-form-item label="题干" prop="content"
        ><el-input v-model="form.content" type="textarea" :rows="3"
      /></el-form-item>
      <el-row :gutter="16"
        ><el-col v-for="key in ['A', 'B', 'C', 'D']" :key="key" :span="12"
          ><el-form-item
            :label="`选项 ${key}`"
            :prop="`option${key}`"
            :rules="[{ required: true, message: `请输入选项${key}` }]"
            ><el-input v-model="form['option' + key]" /></el-form-item></el-col
      ></el-row>
      <el-form-item label="正确答案"
        ><el-radio-group v-model="form.correctOption"
          ><el-radio-button
            v-for="key in ['A', 'B', 'C', 'D']"
            :key="key"
            :value="key"
            >{{ key }}</el-radio-button
          ></el-radio-group
        ></el-form-item
      >
      <el-form-item label="解析"
        ><el-input v-model="form.explanation" type="textarea" :rows="3"
      /></el-form-item> </el-form
    ><template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" @click="save">保存</el-button></template
    >
  </el-dialog>
</template>

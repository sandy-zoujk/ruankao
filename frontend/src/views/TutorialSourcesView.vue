<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { adminTutorialApi } from "../api";
import { sanitizeHtml } from "../utils/sanitizeHtml";
const sources = ref([]),
  loading = ref(false),
  formVisible = ref(false),
  versionVisible = ref(false),
  previewVisible = ref(false),
  formRef = ref(),
  versions = ref([]),
  preview = ref(null),
  selected = ref(null);
const empty = () => ({
    id: null,
    sourceName: "",
    sourceUrl: "",
    chapter: "",
    enabled: true,
  }),
  form = reactive(empty());
async function load() {
  loading.value = true;
  try {
    sources.value = (await adminTutorialApi.sources()).data;
  } finally {
    loading.value = false;
  }
}
function edit(row) {
  Object.assign(form, empty(), row || {});
  formVisible.value = true;
}
async function save() {
  await formRef.value.validate();
  if (form.id) await adminTutorialApi.update(form.id, form);
  else await adminTutorialApi.create(form);
  ElMessage.success("来源已保存");
  formVisible.value = false;
  load();
}
async function sync(row) {
  await ElMessageBox.confirm(
    "系统将下载并清洗该公开网页，结果保存为待审核草稿。确认来源允许使用吗？",
    "同步教程",
    { type: "warning" },
  );
  loading.value = true;
  try {
    const r = await adminTutorialApi.sync(row.id);
    ElMessage.success(
      r.data.changed
        ? `已生成草稿，共 ${r.data.sectionCount} 个小节`
        : "网页内容没有变化",
    );
    await load();
    await showVersions(row);
  } finally {
    loading.value = false;
  }
}
async function showVersions(row) {
  selected.value = row;
  versions.value = (await adminTutorialApi.versions(row.id)).data;
  versionVisible.value = true;
}
async function showPreview(v) {
  preview.value = (
    await adminTutorialApi.preview(selected.value.id, v.id)
  ).data;
  previewVisible.value = true;
}
async function publish(v) {
  await adminTutorialApi.publish(selected.value.id, v.id);
  ElMessage.success("教程已发布");
  versions.value = (await adminTutorialApi.versions(selected.value.id)).data;
  load();
}
async function unpublish(row) {
  await ElMessageBox.confirm(
    "下架后普通用户将无法继续查看该教程。",
    "确认下架",
    { type: "warning" },
  );
  await adminTutorialApi.unpublish(row.id);
  ElMessage.success("教程已下架");
  load();
}
onMounted(load);
</script>
<template>
  <div class="page-actions">
    <div>
      <strong>公开教程来源</strong>
      <p>仅添加确认允许使用的 HTTPS 网页；同步结果审核后才会发布。</p>
    </div>
    <el-button type="primary" @click="edit()">添加来源</el-button>
  </div>
  <section class="panel">
    <el-table v-loading="loading" :data="sources"
      ><el-table-column
        prop="sourceName"
        label="来源"
        width="150"
      /><el-table-column
        prop="chapter"
        label="章节"
        width="170"
      /><el-table-column
        prop="sourceUrl"
        label="网址"
        min-width="260"
        show-overflow-tooltip
      /><el-table-column label="同步状态" width="110"
        ><template #default="{ row }"
          ><el-tag
            :type="
              row.syncStatus === 'SUCCESS'
                ? 'success'
                : row.syncStatus === 'FAILED'
                  ? 'danger'
                  : 'info'
            "
            >{{ row.syncStatus }}</el-tag
          ></template
        ></el-table-column
      ><el-table-column label="版本" width="100"
        ><template #default="{ row }"
          >草稿 {{ row.draftCount }} / 已发布 {{ row.publishedCount }}</template
        ></el-table-column
      ><el-table-column label="操作" width="280"
        ><template #default="{ row }"
          ><el-button link type="primary" @click="sync(row)">同步</el-button
          ><el-button link @click="showVersions(row)">审核版本</el-button
          ><el-button link @click="edit(row)">编辑</el-button
          ><el-button
            v-if="row.publishedCount"
            link
            type="danger"
            @click="unpublish(row)"
            >下架</el-button
          ></template
        ></el-table-column
      ><el-table-column type="expand"
        ><template #default="{ row }"
          ><div class="source-detail">
            <p>最后同步：{{ row.lastSyncedAt || "尚未同步" }}</p>
            <p v-if="row.lastError" class="error-text">
              失败原因：{{ row.lastError }}
            </p>
          </div></template
        ></el-table-column
      ></el-table
    >
  </section>
  <el-dialog
    v-model="formVisible"
    :title="form.id ? '编辑教程来源' : '添加教程来源'"
    width="620px"
    ><el-form
      ref="formRef"
      :model="form"
      :rules="{
        sourceName: [{ required: true, message: '请输入来源名称' }],
        sourceUrl: [
          { required: true, message: '请输入 HTTPS 网址' },
          { pattern: /^https:\/\//i, message: '必须使用 HTTPS' },
        ],
        chapter: [{ required: true, message: '请输入归属章节' }],
      }"
      label-position="top"
      ><el-form-item label="来源名称" prop="sourceName"
        ><el-input v-model="form.sourceName" /></el-form-item
      ><el-form-item label="公开网页地址" prop="sourceUrl"
        ><el-input
          v-model="form.sourceUrl"
          placeholder="https://..." /></el-form-item
      ><el-form-item label="归属章节" prop="chapter"
        ><el-input
          v-model="form.chapter"
          placeholder="例如：项目风险管理" /></el-form-item
      ><el-form-item label="启用"
        ><el-switch v-model="form.enabled" /></el-form-item></el-form
    ><template #footer
      ><el-button @click="formVisible = false">取消</el-button
      ><el-button type="primary" @click="save">保存</el-button></template
    ></el-dialog
  >
  <el-dialog v-model="versionVisible" title="教程版本审核" width="760px"
    ><el-table :data="versions"
      ><el-table-column
        prop="title"
        label="标题"
        min-width="260"
      /><el-table-column
        prop="status"
        label="状态"
        width="110"
      /><el-table-column
        prop="sectionCount"
        label="小节"
        width="80"
      /><el-table-column
        prop="fetchedAt"
        label="抓取时间"
        width="180"
      /><el-table-column label="操作" width="150"
        ><template #default="{ row }"
          ><el-button link @click="showPreview(row)">预览</el-button
          ><el-button
            v-if="row.status === 'DRAFT'"
            link
            type="primary"
            @click="publish(row)"
            >发布</el-button
          ></template
        ></el-table-column
      ></el-table
    ></el-dialog
  >
  <el-dialog
    v-model="previewVisible"
    :title="preview?.title || '教程预览'"
    width="860px"
    ><div v-if="preview">
      <p>{{ preview.summary }}</p>
      <article
        v-for="s in preview.sections"
        :key="s.sectionKey"
        class="tutorial-preview"
      >
        <h3>{{ s.title }}</h3>
        <div v-html="sanitizeHtml(s.contentHtml)"></div>
      </article></div
  ></el-dialog>
</template>

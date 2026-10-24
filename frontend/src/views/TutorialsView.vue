<script setup>
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { tutorialApi } from "../api";
import { sanitizeHtml } from "../utils/sanitizeHtml";
const tutorials = ref([]),
  detail = ref(null),
  summary = reactive({
    sectionCount: 0,
    completedCount: 0,
    progress: 0,
    chapters: [],
  }),
  chapter = ref(""),
  loading = ref(false);
const chapters = computed(() => [
  ...new Set(summary.chapters.map((x) => x.chapter)),
]);
async function load() {
  loading.value = true;
  try {
    const [list, p] = await Promise.all([
      tutorialApi.list(chapter.value),
      tutorialApi.progress(),
    ]);
    tutorials.value = list.data;
    Object.assign(summary, p.data);
  } finally {
    loading.value = false;
  }
}
async function open(row) {
  loading.value = true;
  try {
    detail.value = (await tutorialApi.detail(row.sourceId)).data;
    await nextTick();
    const first = detail.value.sections.findIndex((s) => !s.completed);
    document
      .getElementById(`tutorial-${Math.max(0, first)}`)
      ?.scrollIntoView({ behavior: "smooth" });
  } finally {
    loading.value = false;
  }
}
async function complete(section, value) {
  await tutorialApi.setCompleted(
    detail.value.source.id,
    section.sectionKey,
    value,
  );
  section.completed = value;
  ElMessage.success(value ? "小节已完成" : "已取消完成");
  await load();
}
onMounted(load);
</script>
<template>
  <div v-loading="loading">
    <template v-if="detail">
      <div class="page-actions">
        <div>
          <el-button link type="primary" @click="detail = null"
            >← 返回教程列表</el-button
          >
          <h2>{{ detail.title }}</h2>
          <p>{{ detail.summary }}</p>
        </div>
        <a
          :href="detail.source.sourceUrl"
          target="_blank"
          rel="noopener noreferrer"
          ><el-button>查看原文</el-button></a
        >
      </div>
      <div class="tutorial-layout">
        <aside class="panel tutorial-toc">
          <strong>小节目录</strong
          ><a
            v-for="(s, i) in detail.sections"
            :key="s.sectionKey"
            :href="`#tutorial-${i}`"
            >{{ i + 1 }}. {{ s.title }}</a
          >
        </aside>
        <main>
          <article
            v-for="(s, i) in detail.sections"
            :id="`tutorial-${i}`"
            :key="s.sectionKey"
            class="panel tutorial-section"
          >
            <div class="tutorial-section-head">
              <h3>{{ s.title }}</h3>
              <el-checkbox
                :model-value="!!s.completed"
                @change="(v) => complete(s, v)"
                >已完成</el-checkbox
              >
            </div>
            <div
              class="tutorial-content"
              v-html="sanitizeHtml(s.contentHtml)"
            ></div>
          </article>
        </main>
      </div>
    </template>
    <template v-else>
      <div class="page-actions">
        <div>
          <strong>按章节学习公开授权教程</strong>
          <p>完成小节后自动累计进度，教程内容均保留原始来源。</p>
        </div>
        <el-select
          v-model="chapter"
          clearable
          placeholder="全部章节"
          @change="load"
          ><el-option v-for="c in chapters" :key="c" :label="c" :value="c"
        /></el-select>
      </div>
      <section class="panel tutorial-progress">
        <div>
          <span>整体学习进度</span
          ><strong
            >{{ summary.completedCount }}/{{
              summary.sectionCount
            }}
            小节</strong
          >
        </div>
        <el-progress :percentage="summary.progress" />
      </section>
      <el-empty
        v-if="!tutorials.length"
        description="暂无已发布教程，请联系管理员添加并发布来源"
      />
      <div v-else class="tutorial-grid">
        <article
          v-for="t in tutorials"
          :key="t.sourceId"
          class="panel tutorial-card"
        >
          <el-tag effect="plain">{{ t.chapter }}</el-tag>
          <h3>{{ t.title }}</h3>
          <p>{{ t.summary }}</p>
          <el-progress :percentage="t.progress" />
          <div>
            <small>来源：{{ t.sourceName }}</small
            ><el-button link type="primary" @click="open(t)"
              >继续学习</el-button
            >
          </div>
        </article>
      </div>
    </template>
  </div>
</template>

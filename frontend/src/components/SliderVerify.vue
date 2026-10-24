<script setup>
import { computed, onBeforeUnmount, ref, watch } from "vue";
import { Check } from "@element-plus/icons-vue";

const props = defineProps({ modelValue: Boolean });
const emit = defineEmits(["update:modelValue"]);
const track = ref(),
  offset = ref(0),
  dragging = ref(false);
let startX = 0,
  origin = 0;
const maxOffset = () => Math.max(0, (track.value?.clientWidth || 0) - 44);
const progressWidth = computed(() =>
  props.modelValue ? "100%" : `${offset.value + 44}px`,
);

function start(event) {
  if (props.modelValue) return;
  dragging.value = true;
  startX = event.clientX;
  origin = offset.value;
  window.addEventListener("pointermove", move);
  window.addEventListener("pointerup", end);
}
function move(event) {
  if (!dragging.value) return;
  offset.value = Math.min(
    maxOffset(),
    Math.max(0, origin + event.clientX - startX),
  );
}
function end() {
  if (!dragging.value) return;
  dragging.value = false;
  if (offset.value >= maxOffset() - 4) {
    offset.value = maxOffset();
    emit("update:modelValue", true);
  } else offset.value = 0;
  cleanup();
}
function cleanup() {
  window.removeEventListener("pointermove", move);
  window.removeEventListener("pointerup", end);
}
watch(
  () => props.modelValue,
  (value) => {
    if (!value) offset.value = 0;
  },
);
onBeforeUnmount(cleanup);
</script>

<template>
  <div ref="track" class="slider-track" :class="{ verified: modelValue }">
    <div class="slider-progress" :style="{ width: progressWidth }"></div>
    <span class="slider-label">{{
      modelValue ? "验证通过" : "按住滑块，拖动到最右边"
    }}</span>
    <button
      type="button"
      class="slider-handle"
      :style="{ transform: `translateX(${offset}px)` }"
      @pointerdown.prevent="start"
    >
      <el-icon v-if="modelValue"><Check /></el-icon><span v-else>››</span>
    </button>
  </div>
</template>

<style scoped>
.slider-track {
  position: relative;
  width: 100%;
  height: 44px;
  overflow: hidden;
  border: 1px solid #d9e1de;
  border-radius: 8px;
  background: #f3f6f5;
  user-select: none;
  touch-action: none;
}
.slider-progress {
  position: absolute;
  inset: 0 auto 0 0;
  background: #dceee8;
  transition: width 0.12s;
}
.slider-label {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #7a8984;
  font-size: 13px;
}
.slider-handle {
  position: absolute;
  left: 0;
  top: 0;
  display: grid;
  place-items: center;
  width: 44px;
  height: 42px;
  border: 0;
  border-right: 1px solid #d1dbd7;
  background: white;
  color: #176b61;
  font-size: 17px;
  cursor: grab;
  box-shadow: 2px 0 7px rgba(20, 55, 50, 0.08);
}
.slider-handle:active {
  cursor: grabbing;
}
.verified {
  border-color: #75b5a9;
}
.verified .slider-progress {
  background: #cce8df;
}
.verified .slider-label {
  color: #176b61;
  font-weight: 700;
}
.verified .slider-handle {
  background: #238477;
  color: white;
  border-color: #238477;
}
</style>

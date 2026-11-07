import type { Directive } from 'vue';
import { useAuthStore } from '@/stores/auth';

/** 被占位节点替换的元素 -> 占位注释节点，供 updated 回插与 unmounted 清理使用。 */
const placeholders = new WeakMap<HTMLElement, Comment>();

function applyPermission(el: HTMLElement, permission?: string): void {
  const allowed = !permission || useAuthStore().hasPerm(permission);
  const placeholder = placeholders.get(el);

  if (!allowed) {
    // 无权限：用注释占位节点替换真实元素，DOM 中不存在可点击元素，隐藏样式也无从绕过。
    // 已有占位（重复 mounted/updated）或元素已脱离 DOM 时直接返回，避免重复替换与空指针。
    const parent = el.parentNode;
    if (placeholder || !parent) {
      return;
    }
    const comment = document.createComment('v-auth');
    parent.replaceChild(comment, el);
    placeholders.set(el, comment);
    return;
  }

  // 权限恢复：把原元素回插到占位节点所在位置；占位已被父级移除时不回插，避免挂到已卸载容器。
  if (!placeholder) {
    return;
  }
  const parent = placeholder.parentNode;
  if (parent) {
    parent.replaceChild(el, placeholder);
  }
  placeholders.delete(el);
}

export const authDirective: Directive<HTMLElement, string> = {
  mounted(el, binding) {
    applyPermission(el, binding.value);
  },
  updated(el, binding) {
    applyPermission(el, binding.value);
  },
  unmounted(el) {
    // 元素卸载时若仍被占位，Vue 只持有已脱离 DOM 的 el，占位注释不在其引用链上，
    // 必须主动删除，避免孤儿节点残留；同时清理 WeakMap 记录。
    const placeholder = placeholders.get(el);
    if (placeholder) {
      const parent = placeholder.parentNode;
      if (parent) {
        parent.removeChild(placeholder);
      }
      placeholders.delete(el);
    }
  },
};

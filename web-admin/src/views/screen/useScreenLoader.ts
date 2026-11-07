import { ref, type Ref } from 'vue';

interface ScreenLoader<T> {
  data: Ref<T | null>;
  load: () => Promise<void>;
}

/**
 * 大屏取数：同一时刻只接受最后一次请求的结果，避免快速切换周/月/年时旧响应覆盖新数据。
 */
export function useScreenLoader<T>(fetcher: () => Promise<T>): ScreenLoader<T> {
  const data = ref<T | null>(null) as Ref<T | null>;
  let sequence = 0;

  async function load(): Promise<void> {
    const current = ++sequence;
    try {
      const next = await fetcher();
      if (current === sequence) {
        data.value = next;
      }
    } catch {
      // 保留上一帧数据，等待下一次轮询
    }
  }

  return { data, load };
}

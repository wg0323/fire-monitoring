import { ref } from 'vue';
import { defineStore } from 'pinia';
import { listDict } from '@/api/system';
import type { DictItem } from '@/types/api';

export const useDictStore = defineStore('dict', () => {
  const cache = ref<Record<string, DictItem[]>>({});

  async function load(type: string, force = false): Promise<DictItem[]> {
    if (!force && cache.value[type]) {
      return cache.value[type];
    }
    const items = await listDict(type);
    cache.value[type] = items;
    return items;
  }

  function get(type: string): DictItem[] {
    return cache.value[type] ?? [];
  }

  function clear(): void {
    cache.value = {};
  }

  return { cache, load, get, clear };
});

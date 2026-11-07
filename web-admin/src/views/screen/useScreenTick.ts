import { inject, type Ref } from 'vue';

export const SCREEN_TICK = Symbol('screen-tick');

export function useScreenTick(): Ref<number> {
  const tick = inject<Ref<number> | null>(SCREEN_TICK, null);
  if (!tick) {
    throw new Error('SCREEN_TICK is not provided');
  }
  return tick;
}

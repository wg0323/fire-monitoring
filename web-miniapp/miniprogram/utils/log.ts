/**
 * 错误日志：开发态输出 console，正式版预留上报扩展点，不引入新依赖。
 * @date 2026-11-06
 */

const resolveRelease = (): boolean => {
  try {
    return wx.getAccountInfoSync().miniProgram.envVersion === 'release';
  } catch {
    // 取不到环境信息时按非 release 处理，避免错误被静默吞掉
    return false;
  }
};

const IS_RELEASE = resolveRelease();

/** 统一错误记录：dev/trial 打印到 console.error，release 可在此接入上报 */
export const logError = (scope: string, error: unknown): void => {
  if (!IS_RELEASE) {
    const detail = error instanceof Error ? error.message : String(error);
    console.error(`[${scope}] ${detail}`);
  }
  // release 上报扩展点：接入监控能力时在此追加，调用方无需改动
};

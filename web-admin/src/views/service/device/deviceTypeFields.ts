/**
 * 设备类型 → 独有字段分组。
 *
 * 分组口径（与设备台账表一致）：
 * - aep：独立感烟 / 独立感温 / 可燃气体探测器，新增时必须填写天翼 AEP 接入凭证；
 * - video：双目热成像 / 智能可视化火灾探测报警器，新增时必须填写设备登录账号；
 * - electric：电气火灾探测器，电压、电流、温度、剩余电流、报警浓度由设备上报，表单不填写；
 * - none：水浸、用户信息传输装置、水压、液位等，无独有输入项。
 *
 * 优先按字典 code 匹配，字典改名后仍可命中；code 未收录时按名称关键字兜底。
 * 两边都没命中且已选类型时返回 unknown：视为尚未纳入本映射的新类型，
 * 表单不渲染接入参数，提交时也不覆盖 f_ext 里的既有凭证。
 */
export type DeviceFieldGroup = 'none' | 'aep' | 'video' | 'electric' | 'unknown';

const GROUP_BY_CODE: Record<string, DeviceFieldGroup> = {
  'dh-sa1ga-a': 'aep',
  'dh-sa2ga-a': 'aep',
  'dh-wt2ga': 'aep',
  'dh-gb2ga-a': 'aep',
  'dh-elec': 'electric',
  'dh-cms12gb': 'electric',
  'dh-video': 'video',
  'dh-visual': 'video',
};

const GROUP_BY_NAME: Array<{ pattern: RegExp; group: DeviceFieldGroup }> = [
  { pattern: /热成像|可视化|视频/, group: 'video' },
  { pattern: /电气火灾/, group: 'electric' },
  { pattern: /独立感烟|独立感温|可燃气体|燃气/, group: 'aep' },
];

export function resolveFieldGroup(
  typeCode?: string | null,
  typeName?: string | null,
): DeviceFieldGroup {
  const byCode = typeCode ? GROUP_BY_CODE[typeCode] : undefined;
  if (byCode) {
    return byCode;
  }
  const matched = GROUP_BY_NAME.find((item) => item.pattern.test(typeName ?? ''));
  if (matched) {
    return matched.group;
  }
  return typeCode ? 'unknown' : 'none';
}

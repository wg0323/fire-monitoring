package com.fm.module.system.baseline;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 平台菜单基线：tenant_id=0，一次性覆盖完整 33 页与 P1-P4 按钮权限。
 */
public final class MenuBaseline {

    public static final int TYPE_DIR = 0;
    public static final int TYPE_MENU = 1;
    public static final int TYPE_BUTTON = 2;
    public static final int PLATFORM_ONLY = 1;
    public static final int TENANT_VISIBLE = 0;

    private static final List<MenuSeed> MENUS;

    static {
        List<MenuSeed> menus = new ArrayList<>();
        // 一级目录
        menus.add(dir(1001L, 0L, "数据大屏", "screen", "/screen", 1));
        menus.add(dir(1002L, 0L, "安全值守", "duty", "/duty", 5));
        menus.add(dir(1004L, 0L, "处置跟踪", "trace", "/trace", 7));
        menus.add(dir(1005L, 0L, "工单管理", "workorder", "/workorder", 10));
        menus.add(dir(1006L, 0L, "电子档案", "archive", "/archive", 11));
        menus.add(dir(1007L, 0L, "服务管控", "service", "/service", 13));
        menus.add(dir(1008L, 0L, "统计分析", "stat", "/stat", 17));
        menus.add(dir(1009L, 0L, "安全报告", "report", "/report", 23));
        menus.add(dir(1010L, 0L, "权限管理", "system", "/system", 25));
        menus.add(dir(1011L, 0L, "字典表管理", "dict", "/dict", 30));

        // 二级页面
        menus.add(page(1101L, 1001L, "综合监控", "screen:comprehensive", "/screen/comprehensive", 1));
        menus.add(page(1102L, 1001L, "报警监控", "screen:alarm", "/screen/alarm", 2));
        menus.add(page(1103L, 1001L, "巡查隐患", "screen:danger", "/screen/danger", 3));
        menus.add(page(1104L, 1001L, "设备监控", "screen:device", "/screen/device", 4));
        menus.add(page(1105L, 1002L, "火警处置", "duty:fire", "/duty/fire", 5));
        menus.add(page(1106L, 1002L, "隐患处置", "duty:danger", "/duty/danger", 6));
        menus.add(page(1107L, 1004L, "火警跟踪", "trace:alarm", "/trace/alarm", 7));
        menus.add(page(1108L, 1004L, "火灾跟踪", "trace:fire", "/trace/fire", 8));
        menus.add(page(1109L, 1004L, "隐患跟踪", "trace:danger", "/trace/danger", 9));
        menus.add(page(1110L, 1005L, "工单列表", "workorder:list", "/workorder/list", 10));
        menus.add(page(1111L, 1006L, "水系统", "archive:water", "/archive/water", 11));
        menus.add(page(1112L, 1006L, "电器火灾", "archive:electric", "/archive/electric", 12));
        menus.add(page(1113L, 1007L, "商户管理", "service:merchant", "/service/merchant", 13));
        menus.add(page(1114L, 1007L, "设备管理", "service:device", "/service/device", 14));
        menus.add(page(1115L, 1007L, "巡检管理", "service:inspection", "/service/inspection", 15));
        menus.add(page(1116L, 1007L, "外呼配置", "service:call-config", "/service/call-config", 16));
        menus.add(page(1117L, 1008L, "综合分析", "stat:comprehensive", "/stat/comprehensive", 17));
        menus.add(page(1118L, 1008L, "报警分析", "stat:alarm", "/stat/alarm", 18));
        menus.add(page(1119L, 1008L, "故障分析", "stat:fault", "/stat/fault", 19));
        menus.add(page(1120L, 1008L, "隐患分析", "stat:danger", "/stat/danger", 20));
        menus.add(page(1121L, 1008L, "真警分析", "stat:fire", "/stat/fire", 21));
        menus.add(page(1122L, 1008L, "通话分析", "stat:call", "/stat/call", 22));
        menus.add(page(1123L, 1009L, "月度报告", "report:month", "/report/month", 23));
        menus.add(page(1124L, 1009L, "年度报告", "report:year", "/report/year", 24));
        menus.add(page(1125L, 1010L, "用户管理", "system:user", "/system/user", 25));
        menus.add(page(1126L, 1010L, "工作区管理", "system:work-area", "/system/work-area", 26));
        menus.add(page(1127L, 1010L, "运营中心管理", "system:tenant", "/system/tenant", 27, PLATFORM_ONLY));
        menus.add(page(1128L, 1010L, "角色管理", "system:role", "/system/role", 28));
        menus.add(page(1129L, 1010L, "菜单管理", "system:menu", "/system/menu", 29));
        menus.add(page(1130L, 1011L, "报警类型", "dict:alarm-type", "/dict/alarm-type", 30));
        menus.add(page(1131L, 1011L, "设备类型", "dict:device-type", "/dict/device-type", 31));
        menus.add(page(1132L, 1011L, "商户类型", "dict:merchant-type", "/dict/merchant-type", 32));
        menus.add(page(1133L, 1011L, "隐患类型", "dict:danger-type", "/dict/danger-type", 33));
        menus.add(page(1134L, 1011L, "设备制造商", "dict:device-maker", "/dict/device-maker", 34));

        // 按钮权限
        menus.add(button(1201L, 1125L, "用户编辑", "user:edit", 1));
        menus.add(button(1202L, 1125L, "绑定工作区", "user:bind", 2));
        menus.add(button(1203L, 1126L, "工作区编辑", "work-area:edit", 1));
        menus.add(button(1204L, 1128L, "角色编辑", "role:edit", 1));
        menus.add(button(1205L, 1128L, "分配权限", "role:grant", 2));
        menus.add(button(1206L, 1129L, "菜单编辑", "menu:edit", 1));
        menus.add(button(1207L, 1011L, "字典编辑", "dict:edit", 1));
        menus.add(button(1208L, 1105L, "核警", "alarm:verify", 1));
        menus.add(button(1209L, 1105L, "派单", "alarm:dispatch", 2));
        menus.add(button(1210L, 1105L, "电话核警", "alarm:call", 3));
        menus.add(button(1211L, 1105L, "转隐患", "alarm:convert-danger", 4));
        menus.add(button(1212L, 1106L, "指派隐患", "danger:assign", 1));
        menus.add(button(1213L, 1106L, "审核隐患", "danger:audit", 2));
        menus.add(button(1214L, 1106L, "隐患上报", "danger:report", 3));
        menus.add(button(1215L, 1110L, "接单", "workorder:accept", 1));
        menus.add(button(1216L, 1110L, "完成工单", "workorder:finish", 2));
        menus.add(button(1217L, 1115L, "提交巡查", "inspection:submit", 1));
        menus.add(button(1218L, 1113L, "商户编辑", "merchant:edit", 1));
        menus.add(button(1219L, 1113L, "绑定商户", "merchant:bind", 2));
        menus.add(button(1220L, 1114L, "设备编辑", "device:edit", 1));
        menus.add(button(1221L, 1114L, "批量导入", "device:import", 2));

        MENUS = Collections.unmodifiableList(menus);
    }

    private MenuBaseline() {
    }

    public static List<MenuSeed> list() {
        return MENUS;
    }

    private static MenuSeed dir(Long id, Long parentId, String name, String menuKey, String path, int seq) {
        return new MenuSeed(id, parentId, name, menuKey, TYPE_DIR, path, null, seq, TENANT_VISIBLE);
    }

    private static MenuSeed page(Long id, Long parentId, String name, String menuKey, String path, int seq) {
        return new MenuSeed(id, parentId, name, menuKey, TYPE_MENU, path, null, seq, TENANT_VISIBLE);
    }

    private static MenuSeed page(Long id, Long parentId, String name, String menuKey, String path,
                                 int seq, int platformOnly) {
        return new MenuSeed(id, parentId, name, menuKey, TYPE_MENU, path, null, seq, platformOnly);
    }

    private static MenuSeed button(Long id, Long parentId, String name, String perms, int seq) {
        return new MenuSeed(id, parentId, name, perms, TYPE_BUTTON, null, perms, seq, TENANT_VISIBLE);
    }

    /**
     * 基线菜单种子。
     */
    @Getter
    public static final class MenuSeed {
        private final Long id;
        private final Long parentId;
        private final String name;
        private final String menuKey;
        private final Integer type;
        private final String path;
        private final String perms;
        private final Integer seq;
        private final Integer platformOnly;

        private MenuSeed(Long id, Long parentId, String name, String menuKey, Integer type,
                         String path, String perms, Integer seq, Integer platformOnly) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
            this.menuKey = menuKey;
            this.type = type;
            this.path = path;
            this.perms = perms;
            this.seq = seq;
            this.platformOnly = platformOnly;
        }
    }
}

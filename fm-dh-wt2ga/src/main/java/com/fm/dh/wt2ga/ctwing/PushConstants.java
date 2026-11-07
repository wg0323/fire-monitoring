package com.fm.dh.wt2ga.ctwing;

/**
 * 推送报文中的 messageType / serviceId 与映射产出的类型、事件码常量。
 * 消息类型、Exchange 与路由键与 fm-api 的 MqConstants 逐字一致。
 */
public final class PushConstants {

    public static final String MESSAGE_TYPE_EVENT_REPORT = "eventReport";
    public static final String MESSAGE_TYPE_DATA_REPORT = "dataReport";
    public static final String MESSAGE_TYPE_ONLINE_OFFLINE = "deviceOnlineOfflineReport";

    public static final String SERVICE_TEMPERATURE_STATE = "temperature_state";
    public static final String SERVICE_REMOVE_REPORT = "remove_report";
    public static final String SERVICE_FAULT_CODE_REPORT = "fault_code_report";

    /** serviceId 在 LWM2M 形态下是数字编号，与物模型里的服务编号一致 */
    public static final int SERVICE_CODE_TEMPERATURE_STATE = 1004;
    public static final int SERVICE_CODE_REMOVE_REPORT = 1005;
    public static final int SERVICE_CODE_FAULT_CODE_REPORT = 1008;

    public static final String TYPE_ALARM = "ALARM";
    public static final String TYPE_RECOVER = "RECOVER";
    public static final String TYPE_REALTIME = "REALTIME";
    public static final String TYPE_HEARTBEAT = "HEARTBEAT";

    public static final int EVENT_TYPE_OFFLINE = 0;
    public static final int EVENT_TYPE_ONLINE = 1;

    public static final String ALARM_TYPE_FIRE = "FIRE";
    public static final String ALARM_TYPE_FAULT = "FAULT";
    public static final String ALARM_TYPE_RECOVER = "RECOVER";

    public static final int LEVEL_FIRE = 1;
    public static final int LEVEL_FAULT = 3;

    /** eventCode 记服务标识，不再沿用旧系统写属性值（1 / 5 / 7）的做法 */
    public static final int EVENT_CODE_TEMPERATURE = SERVICE_CODE_TEMPERATURE_STATE;
    public static final int EVENT_CODE_REMOVE = SERVICE_CODE_REMOVE_REPORT;
    public static final int EVENT_CODE_FAULT = SERVICE_CODE_FAULT_CODE_REPORT;

    public static final int TEMPERATURE_STATE_NORMAL = 0;
    public static final int TEMPERATURE_STATE_ALARM = 1;
    public static final int TAMPER_ALARM_OFF = 0;
    public static final int TAMPER_ALARM_ON = 1;

    public static final int FAULT_TEMPERATURE_SENSOR = 5;
    public static final int FAULT_LOW_VOLTAGE = 7;
    public static final int FAULT_LOW_VOLTAGE_RECOVERED = 8;

    public static final String ENUM_TEMPERATURE_STATE = "temperature_state";
    public static final String ENUM_TAMPER_ALARM = "tamper_alarm";
    public static final String ENUM_FAULT = "fault";

    private PushConstants() {
    }
}

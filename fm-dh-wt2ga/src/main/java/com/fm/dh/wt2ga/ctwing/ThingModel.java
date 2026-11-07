package com.fm.dh.wt2ga.ctwing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 物模型翻译：从 thing-model.json 读取服务名与枚举文案，用于日志与 payload.remark。
 * 资源是平台物模型的裁剪版，只保留本采集器会遇到的 serviceId 与枚举。
 */
@Component
public class ThingModel {

    private static final String RESOURCE = "thing-model.json";
    private static final String FIELD_SERVICES = "services";
    private static final String FIELD_ENUMS = "enums";
    private static final String FIELD_IDENTIFIER = "identifier";
    private static final String FIELD_SERVICE_NAME = "serviceName";

    private final Map<String, String> serviceNames = new HashMap<>();
    private final Map<String, Map<String, String>> enumTexts = new HashMap<>();

    public ThingModel(ObjectMapper objectMapper) {
        JsonNode root;
        try (InputStream input = new ClassPathResource(RESOURCE).getInputStream()) {
            root = objectMapper.readTree(input);
        } catch (IOException ex) {
            throw new IllegalStateException("load " + RESOURCE + " failed", ex);
        }
        readServices(root.path(FIELD_SERVICES));
        readEnums(root.path(FIELD_ENUMS));
    }

    public String serviceName(String identifier) {
        String name = serviceNames.get(identifier);
        return name == null ? identifier : name;
    }

    public String enumText(String identifier, Object value) {
        Map<String, String> texts = enumTexts.get(identifier);
        if (texts == null || value == null) {
            return null;
        }
        return texts.get(String.valueOf(value));
    }

    private void readServices(JsonNode services) {
        for (JsonNode service : services) {
            serviceNames.put(service.path(FIELD_IDENTIFIER).asText(), service.path(FIELD_SERVICE_NAME).asText());
        }
    }

    private void readEnums(JsonNode enums) {
        Iterator<Map.Entry<String, JsonNode>> fields = enums.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            Map<String, String> texts = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> values = entry.getValue().fields();
            while (values.hasNext()) {
                Map.Entry<String, JsonNode> value = values.next();
                texts.put(value.getKey(), value.getValue().asText());
            }
            enumTexts.put(entry.getKey(), texts);
        }
    }
}

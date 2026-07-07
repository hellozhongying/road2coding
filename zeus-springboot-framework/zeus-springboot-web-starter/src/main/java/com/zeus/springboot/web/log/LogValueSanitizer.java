package com.zeus.springboot.web.log;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zeus.springboot.web.annotation.LogMask;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * API 日志值脱敏器。
 *
 * <p>在保留对象结构的前提下，递归处理集合、数组、Map 和普通对象，并替换 {@link LogMask} 标记的成员。</p>
 */
class LogValueSanitizer {

    private final ObjectMapper objectMapper;

    private final String maskText;

    LogValueSanitizer(ObjectMapper objectMapper, String maskText) {
        this.objectMapper = objectMapper;
        this.maskText = maskText;
    }

    Object sanitize(Object value) {
        return sanitize(value, new IdentityHashMap<>());
    }

    private Object sanitize(Object value, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || isSimpleValue(value)) {
            return value;
        }
        if (value instanceof JsonNode jsonNode) {
            return jsonNode.deepCopy();
        }
        if (visited.containsKey(value)) {
            // 对象图存在循环引用时停止递归，避免日志序列化栈溢出。
            return String.valueOf(value);
        }
        visited.put(value, Boolean.TRUE);

        if (value instanceof Iterable<?> iterable) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (Object item : iterable) {
                arrayNode.add(objectMapper.valueToTree(sanitize(item, visited)));
            }
            return arrayNode;
        }
        if (value.getClass().isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                arrayNode.add(objectMapper.valueToTree(sanitize(Array.get(value, index), visited)));
            }
            return arrayNode;
        }
        if (value instanceof Map<?, ?> map) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                objectNode.set(String.valueOf(entry.getKey()), objectMapper.valueToTree(sanitize(entry.getValue(), visited)));
            }
            return objectNode;
        }

        ObjectNode node = objectMapper.valueToTree(value);
        // Jackson 先生成基础 JSON，再用反射递归替换字段值，以便嵌套对象同样应用脱敏规则。
        sanitizeFields(value, node, visited);
        maskAnnotatedMembers(value.getClass(), node);
        return node;
    }

    private void sanitizeFields(Object value, ObjectNode objectNode, IdentityHashMap<Object, Boolean> visited) {
        for (Class<?> current = value.getClass(); current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isAnnotationPresent(LogMask.class)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object fieldValue = field.get(value);
                    if (fieldValue != null) {
                        objectNode.set(resolveJsonName(field), objectMapper.valueToTree(sanitize(fieldValue, visited)));
                    }
                } catch (IllegalAccessException ignored) {
                    // 反射访问失败时保留 Jackson 原始序列化值，避免日志脱敏流程影响主链路。
                }
            }
        }
    }

    private void maskAnnotatedMembers(Class<?> valueType, JsonNode node) {
        if (!(node instanceof ObjectNode objectNode)) {
            return;
        }
        // 字段和 getter 都支持 @LogMask，便于适配字段访问和 JavaBean 两种模型。
        for (Class<?> current = valueType; current != null && current != Object.class; current = current.getSuperclass()) {
            maskAnnotatedFields(current, objectNode);
            maskAnnotatedGetters(current, objectNode);
        }
    }

    private void maskAnnotatedFields(Class<?> valueType, ObjectNode objectNode) {
        for (Field field : valueType.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !field.isAnnotationPresent(LogMask.class)) {
                continue;
            }
            objectNode.put(resolveJsonName(field), maskText);
        }
    }

    private void maskAnnotatedGetters(Class<?> valueType, ObjectNode objectNode) {
        for (Method method : valueType.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 0
                    || !method.isAnnotationPresent(LogMask.class)) {
                continue;
            }
            String propertyName = resolveJsonName(method);
            if (propertyName != null) {
                objectNode.put(propertyName, maskText);
            }
        }
    }

    private String resolveJsonName(Field field) {
        JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isBlank()) {
            return jsonProperty.value();
        }
        return field.getName();
    }

    private String resolveJsonName(Method method) {
        JsonProperty jsonProperty = method.getAnnotation(JsonProperty.class);
        if (jsonProperty != null && !jsonProperty.value().isBlank()) {
            return jsonProperty.value();
        }
        String methodName = method.getName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }
        return null;
    }

    private String decapitalize(String value) {
        if (value.length() > 1 && Character.isUpperCase(value.charAt(0)) && Character.isUpperCase(value.charAt(1))) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private boolean isSimpleValue(Object value) {
        Class<?> valueType = value.getClass();
        return valueType.isPrimitive()
                || value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>
                || valueType.getName().startsWith("java.time.");
    }
}

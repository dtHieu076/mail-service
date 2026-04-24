package org.atlas_erp.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

public class SafeJsonDeserializer<T> implements Deserializer<T> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Class<T> type;

    @SuppressWarnings("unchecked")
    @Override
    public void configure(java.util.Map<String, ?> configs, boolean isKey) {
        if (type == null) {
            String className = (String) configs.get("json.value.type");
            try {
                this.type = (Class<T>) Class.forName(className);
            } catch (Exception e) {
                throw new RuntimeException("Cannot load type for deserializer", e);
            }
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {

        // 1. CHẶN RỖNG
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            // 2. PARSE JSON
            return objectMapper.readValue(data, type);

        } catch (Exception e) {
            // 3. LOG & SKIP (KHÔNG THROW)
            System.err.println("[Kafka] Invalid JSON on topic " + topic
                    + " -> " + e.getMessage());

            return null;
        }
    }
}
package org.atlas_erp.Config;

import io.smallrye.reactive.messaging.kafka.DeserializationFailureHandler;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.common.header.Headers;
import io.quarkus.logging.Log;

@ApplicationScoped
@Identifier("failure-fallback") // Đặt tên cho handler này
public class MyDeserializationFailureHandler implements DeserializationFailureHandler<Object> {
    @Override
    public Object handleDeserializationFailure(String topic, boolean isKey, String deserializer, byte[] data,
            Exception exception, Headers headers) {
        // Log lỗi ra để biết offset nào bị hỏng
        Log.error("Lỗi giải mã trên topic: " + topic + ". Dữ liệu thô: " + new String(data));

        // Trả về null để Bước 3 (hàm consume) nhận được giá trị null và chạy lệnh if
        // của bạn
        return null;
    }
}

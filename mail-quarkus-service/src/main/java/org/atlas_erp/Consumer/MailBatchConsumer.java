package org.atlas_erp.Consumer;

import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.atlas_erp.Service.MailProcessorService;
import org.atlas_erp.dto.MailBatchEventDTO;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@ApplicationScoped
public class MailBatchConsumer {

    private final MailProcessorService mailProcessorService;

    @Inject
    public MailBatchConsumer(MailProcessorService mailProcessorService) {
        this.mailProcessorService = mailProcessorService;
    }

    @Incoming("mail-batch-requests-in")
    // @Blocking(value = "mail-db-pool", ordered = false) // Cho phép xử lý song
    // song các batch khác nhau
    @Blocking(ordered = true) // Đảm bảo thứ tự xử lý batch theo campaign
    public void consumeBatch(MailBatchEventDTO batchEvent) {
        if (batchEvent == null || batchEvent.getBatchRecipients() == null) {
            return;
        }

        try {
            // Đẩy vào service để chạy logic @Transactional
            mailProcessorService.processBatch(batchEvent);
            Log.infof("Da xu ly batch %d recipients cho Campaign %s", batchEvent.getBatchRecipients().size(),
                    batchEvent.getCampaignId());
        } catch (Exception e) {
            Log.errorf("Lỗi insert DB cho Campaign %s: %s", batchEvent.getCampaignId(), e.getMessage());
            throw e; // Ném lỗi để Kafka không ACK nếu chưa cấu hình Dead Letter Queue
        }
    }
}
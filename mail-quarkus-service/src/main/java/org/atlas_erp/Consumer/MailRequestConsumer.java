package org.atlas_erp.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.atlas_erp.Entity.MailCampaign;
import org.atlas_erp.Service.MailProcessorService;
import org.atlas_erp.dto.MailBatchEventDTO;
import org.atlas_erp.dto.MailEventDTO;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.List;

@ApplicationScoped
public class MailRequestConsumer {

    private final ObjectMapper objectMapper;
    private final MailProcessorService mailProcessorService;

    // Bắn payload trung gian ra Topic mới
    @Inject
    @Channel("mail-batch-requests-out")
    Emitter<MailBatchEventDTO> batchEmitter;

    public MailRequestConsumer(MailProcessorService mailProcessorService, ObjectMapper objectMapper) {
        this.mailProcessorService = mailProcessorService;
        this.objectMapper = objectMapper;
    }

    @Incoming("mail-requests")
    public void consume(MailEventDTO event) throws Exception {

        if (event == null || event.getRecipients() == null || event.getRecipients().isEmpty()) {
            Log.warn("Event không hợp lệ");
            return;
        }

        // 1. Tạo campaign (được quản lý Transaction trong Service)
        MailCampaign campaign = mailProcessorService.createCampaign(event);
        String jsonData = objectMapper.writeValueAsString(event.getTemplateData());

        int batchSize = 20;
        List<MailEventDTO.Recipient> recipients = event.getRecipients();

        // 2. Chia nhỏ và bắn ngay ra Kafka thay vì xử lý tuần tự
        for (int i = 0; i < recipients.size(); i += batchSize) {
            List<MailEventDTO.Recipient> batch = recipients.subList(i, Math.min(i + batchSize, recipients.size()));

            MailBatchEventDTO batchEvent = new MailBatchEventDTO(
                    campaign.getId(),
                    event.getTemplateId(),
                    jsonData,
                    batch);

            batchEmitter.send(batchEvent);
            // Log.infof("Da day batch %d users của Campaign %s ra topic xu ly con", batch.size(), campaign.getId());
        }
    }
}
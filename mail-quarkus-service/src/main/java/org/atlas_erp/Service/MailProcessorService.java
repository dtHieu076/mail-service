package org.atlas_erp.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;
import org.atlas_erp.Entity.MailCampaign;
import org.atlas_erp.Entity.MailJob;
import org.atlas_erp.Repository.MailCampaignRepository;
import org.atlas_erp.Repository.MailJobRepository;
import org.atlas_erp.dto.MailBatchEventDTO;
import org.atlas_erp.dto.MailEventDTO;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MailProcessorService {

    private final MailCampaignRepository campaignRepo;
    private final MailJobRepository jobRepo;
    private final ObjectMapper objectMapper;

    @Inject
    @Channel("internal-jobs-out")
    Emitter<String> jobEmitter;

    @Inject
    TransactionManager tm;

    public MailProcessorService(MailCampaignRepository campaignRepo, MailJobRepository jobRepo,
            ObjectMapper objectMapper) {
        this.campaignRepo = campaignRepo;
        this.jobRepo = jobRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MailCampaign createCampaign(MailEventDTO event) {
        MailCampaign campaign = new MailCampaign();
        campaign.setTemplateId(event.getTemplateId());
        campaign.setSendType(event.getSendType());
        campaign.setRecipientsCount(event.getRecipients().size());
        campaign.setPendingCount(event.getRecipients().size());

        campaignRepo.persist(campaign);
        return campaign; // Sẽ tự sinh UUID do @GeneratedValue
    }

    @Transactional
    public void processBatch(MailBatchEventDTO batchEvent) {
        // 1. Phục hồi lại đối tượng campaign từ DB (để Hibernate quản lý mapping)
        MailCampaign campaign = campaignRepo.getEntityManager().getReference(MailCampaign.class,
                batchEvent.getCampaignId());
        if (campaign == null) {
            Log.error("Campaign không tồn tại: " + batchEvent.getCampaignId());
            return;
        }

        List<MailJob> jobs = new ArrayList<>();
        List<Long> jobIds = new ArrayList<>();
        Map<String, Object> baseData;

        try {
            // Dùng lại jsonData được truyền từ Consumer 1
            baseData = objectMapper.readValue(
                    batchEvent.getJsonData(),
                    new TypeReference<Map<String, Object>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException("Parse templateData failed", e);
        }

        // 2. Map dữ liệu
        for (MailEventDTO.Recipient r : batchEvent.getBatchRecipients()) {
            Map<String, Object> data = new HashMap<>(baseData);
            data.put("userName", r.getName());

            MailJob job = new MailJob();
            job.setCampaign(campaign);
            job.setRecipientEmail(r.getEmail());
            job.setTemplateId(batchEvent.getTemplateId()); // Lấy templateId từ batchEvent

            try {
                job.setTemplateData(objectMapper.writeValueAsString(data));
            } catch (Exception e) {
                throw new RuntimeException("Serialize templateData failed", e);
            }

            job.setStatus(MailJob.MailJobStatus.PENDING);
            jobs.add(job);
        }

        // 3. Batch Insert
        jobRepo.persist(jobs);
        jobRepo.flush();

        for (MailJob job : jobs) {
            jobIds.add(job.getId());
        }

        // 4. Bắn event khi transaction commit thành công
        try {
            tm.getTransaction().registerSynchronization(new jakarta.transaction.Synchronization() {
                @Override
                public void beforeCompletion() {
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == Status.STATUS_COMMITTED) {
                        try {
                            String payload = objectMapper.writeValueAsString(jobIds);
                            jobEmitter.send(payload);
                            Log.infof("Emit batch %d jobs thanh cong", jobIds.size());
                        } catch (Exception e) {
                            Log.error("Lỗi serialize jobIds", e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.error("Lỗi register transaction sync", e);
        }
    }
}
package org.atlas_erp.MailWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.atlas_erp.Entity.MailJob;
import org.atlas_erp.Repository.MailCampaignRepository;
import org.atlas_erp.Repository.MailJobRepository;
import org.atlas_erp.Service.TemplateService;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class MailWorker {

    private final MailJobRepository jobRepo;
    private final MailCampaignRepository campaignRepo;
    private final TemplateService templateService;
    private final Mailer mailer;
    private final ObjectMapper objectMapper;

    private static final String FROM_EMAIL = "DangThanhHieu@atlas-erp.com";

    public MailWorker(MailJobRepository jobRepo,
            MailCampaignRepository campaignRepo,
            TemplateService templateService,
            Mailer mailer,
            ObjectMapper objectMapper) {
        this.jobRepo = jobRepo;
        this.campaignRepo = campaignRepo;
        this.templateService = templateService;
        this.mailer = mailer;
        this.objectMapper = objectMapper;
    }

    @Incoming("internal-jobs-in")
    @Blocking(ordered = false)
    public void processBatch(String payload) {

        try {
            List<Long> jobIds = objectMapper.readValue(
                    payload,
                    new TypeReference<List<Long>>() {
                    });

            List<MailJob> jobs = getJobs(jobIds);

            if (jobs.isEmpty()) {
                return;
            }

            List<Long> successIds = new ArrayList<>();
            List<Long> failIds = new ArrayList<>();

            UUID campaignId = jobs.get(0).getCampaign().getId();

            for (MailJob job : jobs) {
                try {
                    String html = templateService.render(job);

                    mailer.send(
                            Mail.withHtml(job.getRecipientEmail(),
                                    "Atlas ERP Notification",
                                    html)
                                    .setFrom(FROM_EMAIL));

                    successIds.add(job.getId());

                } catch (Exception e) {
                    failIds.add(job.getId());
                }
            }

            updateBatch(successIds, failIds, campaignId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // DB METHODS
    // =========================

    @Transactional
    public List<MailJob> getJobs(List<Long> ids) {
        return jobRepo.list("id IN ?1", ids);
    }

    @Transactional
    public void updateBatch(List<Long> successIds,
            List<Long> failIds,
            UUID campaignId) {

        if (!successIds.isEmpty()) {
            jobRepo.update("status = 'SENT' WHERE id IN ?1", successIds);
        }

        if (!failIds.isEmpty()) {
            jobRepo.update("status = 'FAILED' WHERE id IN ?1", failIds);
        }

        int success = successIds.size();
        int fail = failIds.size();

        campaignRepo.update(
                "successCount = successCount + ?1, " +
                        "failedCount = failedCount + ?2, " +
                        "pendingCount = pendingCount - (?1 + ?2) " +
                        "WHERE id = ?3",
                success, fail, campaignId);

        campaignRepo.update(
                "status = 'COMPLETED' WHERE id = ?1 AND pendingCount <= 0",
                campaignId);
    }
}
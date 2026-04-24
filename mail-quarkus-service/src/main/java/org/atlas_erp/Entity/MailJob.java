package org.atlas_erp.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;

@Entity
@Table(name = "mail_jobs", indexes = {
        @Index(name = "idx_status_created", columnList = "status, createdAt")
})
public class MailJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private String recipientEmail;
    private String templateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String templateData;

    @Enumerated(EnumType.STRING)
    private MailJobStatus status = MailJobStatus.PENDING;

    private int retryCount;
    private String errorMessage;

    private LocalDateTime createdAt;

    public Long getId() {
        return Id;
    }

    public MailCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(MailCampaign campaign) {
        this.campaign = campaign;
    }

    public void setId(Long id) {
        Id = id;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateData() {
        return templateData;
    }

    public void setTemplateData(String templateData) {
        this.templateData = templateData;
    }

    public MailJobStatus getStatus() {
        return status;
    }

    public void setStatus(MailJobStatus status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private MailCampaign campaign;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum MailJobStatus {
        PENDING, PROCESSING, SENT, FAILED, RETRYING, DEAD_LETTER
    }
}

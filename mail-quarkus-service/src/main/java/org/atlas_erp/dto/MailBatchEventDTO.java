package org.atlas_erp.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class MailBatchEventDTO {
    private UUID campaignId;
    private String templateId;
    private String jsonData;
    private List<MailEventDTO.Recipient> batchRecipients;
}
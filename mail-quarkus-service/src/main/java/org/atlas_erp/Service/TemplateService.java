package org.atlas_erp.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.atlas_erp.Entity.MailJob;
import org.jboss.logging.Logger;

import java.util.Map;

@ApplicationScoped
public class TemplateService {
    private static final Logger LOG = Logger.getLogger(TemplateService.class);

    @Inject
    Engine quteEngine;

    @Inject
    ObjectMapper objectMapper;

    // Render nội dung HTML từ file template (trong src/main/resources/templates)
    public String render(MailJob job) {
        try {
            // Parse JSON từ DB sang Map
            Map<String, Object> data = objectMapper.readValue(
                    job.getTemplateData(),
                    new TypeReference<Map<String, Object>>() {
                    });

            // Tìm file template dựa vào templateId (ví dụ: "welcome-email" ->
            // "welcome-email.html")
            Template template = quteEngine.getTemplate(job.getTemplateId());
            if (template == null) {
                throw new IllegalArgumentException("Template not found: " + job.getTemplateId());
            }

            // Gắn dữ liệu và render ra HTML
            return template.data(data).render();
        } catch (Exception e) {
            LOG.errorf("Error rendering template for job %s: %s", job.getTemplateId(), e.getMessage());
            throw new RuntimeException("Failed to render template", e);
        }
    }
}

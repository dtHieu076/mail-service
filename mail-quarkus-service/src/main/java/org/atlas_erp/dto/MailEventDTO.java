package org.atlas_erp.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RegisterForReflection giúp GraalVM giữ lại metadata của class này khi build
 * Native.
 * Nếu không có, Jackson có thể không tìm thấy các Getter/Setter trong bản
 * Native.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class MailEventDTO {

    @NotBlank(message = "Template ID is required")
    private String templateId;

    @NotNull(message = "Template data cannot be null")
    private Map<String, Object> templateData;

    @NotEmpty(message = "Recipients list cannot be empty")
    private List<@Valid Recipient> recipients;

    private List<String> tags;

    // Sử dụng giá trị mặc định trực tiếp
    private String sendType = "INDIVIDUAL";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @RegisterForReflection
    public static class Recipient {

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format") // Kiểm tra định dạng email chuẩn
        private String email;

        private String name;
    }
}
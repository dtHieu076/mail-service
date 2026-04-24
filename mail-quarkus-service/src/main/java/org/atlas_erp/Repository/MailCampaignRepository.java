package org.atlas_erp.Repository;

import java.util.UUID;
import org.atlas_erp.Entity.MailCampaign;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MailCampaignRepository implements PanacheRepositoryBase<MailCampaign, UUID> {
    // Bây giờ hàm findById(UUID) sẽ hoạt động bình thường
}
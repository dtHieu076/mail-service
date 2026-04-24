package org.atlas_erp.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.atlas_erp.Entity.MailJob;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MailJobRepository implements PanacheRepository<MailJob> {
    public List<MailJob> findStuckJobs(LocalDateTime time) {
        return find("status = ?1 and createdAt < ?2",
                MailJob.MailJobStatus.PENDING, time).list();
    }
}

package com.easy1auth.audit.repository;
import com.easy1auth.audit.model.WebhookSubscriptionEntity;
import com.easy1auth.audit.model.WebhookSubscriptionEntityTable;
import org.babyfish.jimmer.spring.repository.JRepository;
import java.util.UUID;
public interface DeliveryRepository extends JRepository<WebhookSubscriptionEntity, UUID> {
    WebhookSubscriptionEntityTable HOOK = WebhookSubscriptionEntityTable.$;
}

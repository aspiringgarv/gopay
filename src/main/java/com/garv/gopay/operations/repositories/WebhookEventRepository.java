package com.garv.gopay.operations.repositories;

import com.garv.gopay.common.enums.WebhookEventStatus;
import com.garv.gopay.operations.entities.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    List<WebhookEvent> findFirst500ByStatusOrderByCreatedAtAsc(WebhookEventStatus status);

}

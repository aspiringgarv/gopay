package com.garv.gopay.payment.repositories;

import com.garv.gopay.common.enums.OutboxEventStatus;
import com.garv.gopay.payment.entities.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findFirst500ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);

}

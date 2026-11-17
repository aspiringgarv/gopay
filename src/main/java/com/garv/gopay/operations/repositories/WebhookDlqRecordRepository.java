package com.garv.gopay.operations.repositories;

import com.garv.gopay.operations.entities.WebhookDlqRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WebhookDlqRecordRepository extends JpaRepository<WebhookDlqRecord, UUID> {
}

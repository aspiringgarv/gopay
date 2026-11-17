package com.garv.gopay.merchant.entities;

import com.garv.gopay.common.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;
import java.util.UUID;

@Entity
@Table(name = "merchant_webhook_config", indexes = {
        @Index(name = "idx_merchant_webhook_config_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_merchant_webhook_config_merchant_id_enabled", columnList = "merchant_id, enabled")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantWebhookConfig extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false)
    private String targetUrl;

    @Column(length = 255)
    private String eventTypeFilter;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    private byte[] encryptedSecret;

    @Column(nullable = false)
    private byte[] encryptedDek;

    public boolean isSubscribed(String eventType) {
        if (eventTypeFilter == null || eventTypeFilter.isBlank()) {
            return true;
        }
        return Arrays.asList(eventTypeFilter.split(",")).contains(eventType);
    }

}

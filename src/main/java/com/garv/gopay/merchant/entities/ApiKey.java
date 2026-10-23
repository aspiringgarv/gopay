package com.garv.gopay.merchant.entities;

import com.garv.gopay.common.entities.BaseEntity;
import com.garv.gopay.common.enums.Environment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_key", indexes = {
        @Index(name = "idx_api_key_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_api_key_merchant_id_environment_enabled", columnList = "merchant_id, environment, enabled")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(unique = true, nullable = false, length = 50)
    private String keyId;

    @Column(nullable = false,length =  200)
    private String keySecretHash;

    @Column
    private String previousKeySecretHash;

    @Column
    private String webhookSecretHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Environment environment;

    @Column(nullable = false)
    private boolean enabled;

    private LocalDateTime lastUsedAt;

    private LocalDateTime rotatedAt;

    private LocalDateTime gracePeriodExpiresAt;

    public boolean isInGracePeriod() {
        return gracePeriodExpiresAt != null && LocalDateTime.now().isBefore(gracePeriodExpiresAt);
    }

}

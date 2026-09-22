package com.garv.gopay.merchant.entities;

import com.garv.gopay.common.enums.Environment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_key")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {
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

    private LocalDateTime createdAt;

    private LocalDateTime rotatedAt;

    private LocalDateTime gracePeriodExpiresAt;

}

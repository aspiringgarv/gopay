package com.garv.gopay.vault.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vault_card")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultCard {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private byte[] encryptedPan;

    @Column(nullable = false)
    private byte[] encryptedDek;

    @Column(nullable = false, length = 4)
    private String lastFour;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String bin;

    @Column(nullable = false)
    private int expiryMonth;

    @Column(nullable = false)
    private int expiryYear;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}

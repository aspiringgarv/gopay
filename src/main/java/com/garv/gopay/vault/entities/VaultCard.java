package com.garv.gopay.vault.entities;

import com.garv.gopay.common.entities.BaseEntity;
import com.garv.gopay.common.enums.CardBrand;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "vault_card")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VaultCard extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private byte[] encryptedPan;

    @Column(nullable = false)
    private byte[] encryptedDek;

    @Column(nullable = false, length = 4)
    private String lastFour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardBrand brand;

    @Column(nullable = false)
    private String bin;

    @Column(nullable = false)
    private int expiryMonth;

    @Column(nullable = false)
    private int expiryYear;

}

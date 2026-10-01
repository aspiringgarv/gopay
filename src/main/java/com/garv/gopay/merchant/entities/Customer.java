package com.garv.gopay.merchant.entities;

import com.garv.gopay.common.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "customer", indexes = {
        @Index(name = "idx_customer_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_customer_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 200)
    private String name;

    @Column(nullable = false)
    private String email;   // Customer can hame many merchant so same email in any merchant at hn and zara at the same time

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String gstId;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

}

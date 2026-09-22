package com.garv.gopay.merchant.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}

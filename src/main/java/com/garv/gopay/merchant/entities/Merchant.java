package com.garv.gopay.merchant.entities;

import com.garv.gopay.common.enums.BuisnessType;
import com.garv.gopay.common.enums.MerchantStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name",nullable = false, length = 200)
    private String name;

    @Column(name = "buisness_type",length = 200)
    @Enumerated(EnumType.STRING)
    private BuisnessType buisnessType;

    @Column(name = "business_name")
    private String businessName;

    @Column(nullable = false)
    private String merchantId;

    @Column(name = "merchant_status",nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MerchantStatus merchantStatus = MerchantStatus.PENDING_KYC;

    @Column(name = "email",unique = true,nullable = false)
    private String email;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "gst_id",length = 20)
    private String gstId;

    @Column(name = "pan",length = 20)
    private String pan;

    @Column(name = "settlement_bank_account")
    private String settlementBankAccount;

    @Column(name = "settlement_ifsc")
    private String settlementIfsc;

    @Column(name = "settlement_account_holder_name")
    private String settlementAccountHolderName;
}

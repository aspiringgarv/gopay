package com.garv.gopay.vault.services.impl;

import com.garv.gopay.common.enums.CardBrand;
import com.garv.gopay.common.exceptions.BusinessRuleViolationException;
import com.garv.gopay.common.exceptions.ResourceNotFoundException;
import com.garv.gopay.common.util.RandomizerUtil;
import com.garv.gopay.payment.gateway.dto.PaymentRequest;
import com.garv.gopay.payment.processor.dto.PaymentProcessorRequest;
import com.garv.gopay.payment.processor.dto.PaymentProcessorResponse;
import com.garv.gopay.payment.processor.router.PaymentProcessorRouter;
import com.garv.gopay.vault.config.VaultEncryptionConfig;
import com.garv.gopay.vault.converters.cardtoken.CardTokenConverter;
import com.garv.gopay.vault.converters.vaultcard.VaultCardConverter;
import com.garv.gopay.vault.dto.request.TokenizeCardRequest;
import com.garv.gopay.vault.dto.response.TokenizeCardResponse;
import com.garv.gopay.vault.entities.CardToken;
import com.garv.gopay.vault.entities.VaultCard;
import com.garv.gopay.vault.repositories.CardTokenRepository;
import com.garv.gopay.vault.repositories.VaultCardRepository;
import com.garv.gopay.vault.services.VaultService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VaultServiceImpl implements VaultService {

    private final VaultCardRepository vaultCardRepository;
    private final CardTokenRepository cardTokenRepository;
    private final VaultCardConverter vaultCardConverter;
    private final CardTokenConverter cardTokenConverter;
    private final VaultEncryptionConfig vaultEncryptionConfig;
    private final BytesEncryptor vaultMasterEncryptor;
    private final PaymentProcessorRouter paymentProcessorRouter;

    @Override
    @Transactional
    public TokenizeCardResponse tokenize(@NonNull TokenizeCardRequest request, @NonNull UUID merchantId) {
        // request.cvv() is intentionally never read here — PCI-DSS forbids storing CVV after authorization
        String pan = request.pan();
        String lastFour = pan.substring(pan.length() - 4);
        String bin = pan.substring(0, Math.min(6, pan.length()));
        CardBrand brand = CardBrand.fromPan(pan);

        byte[] dek = vaultEncryptionConfig.vaultDekGenerator().generateKey();
        BytesEncryptor panEncryptor = vaultEncryptionConfig.panEncryptor(dek);

        byte[] encryptedPan = panEncryptor.encrypt(pan.getBytes(StandardCharsets.UTF_8));
        byte[] encryptedDek = vaultMasterEncryptor.encrypt(dek);

        VaultCard vaultCard = vaultCardConverter.toEntity(encryptedPan, encryptedDek, lastFour, brand, bin, request);
        vaultCard = vaultCardRepository.save(vaultCard);

        String token = "tok_" + RandomizerUtil.randomBase64(24);
        CardToken cardToken = cardTokenConverter.toEntity(token, vaultCard, request.customerId(), merchantId);
        cardTokenRepository.save(cardToken);

        return new TokenizeCardResponse(token, lastFour, brand, request.expiryMonth(), request.expiryYear());
    }

    @Override
    public PaymentProcessorResponse charge(@NonNull String token, @NonNull PaymentRequest paymentRequest) {
        CardToken cardToken = cardTokenRepository.findByTokenAndRevokedAtIsNull(token)
                .orElseThrow(() -> new ResourceNotFoundException("CardToken", token));

        VaultCard vaultCard = cardToken.getVaultCard();

        YearMonth cardExpiry = YearMonth.of(vaultCard.getExpiryYear(), vaultCard.getExpiryMonth());
        if (cardExpiry.isBefore(YearMonth.now())) {
            throw new BusinessRuleViolationException("CARD_EXPIRED", "Cannot charge an expired card: " + token);
        }

        byte[] dek = null;
        byte[] panBytes = null;
        try {
            dek = vaultMasterEncryptor.decrypt(vaultCard.getEncryptedDek());
            // pan is decrypted only to hand it to the real card network — never log/persist it in plaintext
            panBytes = vaultEncryptionConfig.panEncryptor(dek).decrypt(vaultCard.getEncryptedPan());
            String pan = new String(panBytes, StandardCharsets.UTF_8);
            String expiry = vaultCard.getExpiryMonth() + "/" + vaultCard.getExpiryYear();

            PaymentProcessorRequest processorRequest = PaymentProcessorRequest.forCard(
                    paymentRequest.paymentId(),
                    UUID.randomUUID(),
                    paymentRequest.method(),
                    paymentRequest.amount(),
                    pan,
                    expiry,
                    paymentRequest.methodDetails()
            );

            return paymentProcessorRouter.charge(processorRequest);
        } catch (Exception ex) {
            log.error("Card charge failed for paymentId {}", paymentRequest.paymentId(), ex);
            return new PaymentProcessorResponse.Failure("CARD_CHARGE_ERROR", ex.getMessage());
        } finally {
            if (panBytes != null) {
                Arrays.fill(panBytes, (byte) 0);
            }
            if (dek != null) {
                Arrays.fill(dek, (byte) 0);
            }
        }
    }

}

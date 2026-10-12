package com.garv.gopay.vault.dto.response;

import com.garv.gopay.common.enums.CardBrand;

public record TokenizeCardResponse(
        String token,
        String lastFour,
        CardBrand brand,
        Integer expiryMonth,
        Integer expiryYear
) {
}

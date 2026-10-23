package com.garv.gopay.common.enums;

public enum CardBrand {
    VISA,
    MASTERCARD,
    AMEX,
    DISCOVER,
    UNKNOWN;

    private static final String VISA_PREFIX = "4";
    private static final String MASTERCARD_PREFIX = "5";
    private static final String AMEX_PREFIX_1 = "34";
    private static final String AMEX_PREFIX_2 = "37";
    private static final String DISCOVER_PREFIX = "6";

    public static CardBrand fromPan(String pan) {
        if (pan.startsWith(VISA_PREFIX)) {
            return VISA;
        }
        if (pan.startsWith(MASTERCARD_PREFIX)) {
            return MASTERCARD;
        }
        if (pan.startsWith(AMEX_PREFIX_1) || pan.startsWith(AMEX_PREFIX_2)) {
            return AMEX;
        }
        if (pan.startsWith(DISCOVER_PREFIX)) {
            return DISCOVER;
        }
        return UNKNOWN;
    }

}

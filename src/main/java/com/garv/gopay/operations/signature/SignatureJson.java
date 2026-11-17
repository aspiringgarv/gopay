package com.garv.gopay.operations.signature;


import lombok.SneakyThrows;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class SignatureJson {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SignatureJson() {
    }

    @SneakyThrows
    public static String toJson(SignatureData data) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("eventId", data.eventId().toString());
        canonical.put("timestamp", data.timestamp());
        canonical.put("payload", new TreeMap<>(data.payload()));
        return OBJECT_MAPPER.writeValueAsString(canonical);
    }

}

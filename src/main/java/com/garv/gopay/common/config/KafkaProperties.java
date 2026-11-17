package com.garv.gopay.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

// topic names live here instead of hardcoded in service code, keyed by aggregate type
// (e.g. "payment" -> "payment-events") so a topic rename is a yaml change, not a code change
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {

    private Map<String, String> topics;

    public String getTopic(String aggregateType) {
        String topic = topics.get(aggregateType.toLowerCase());
        if (topic == null) {
            throw new IllegalStateException("No kafka topic configured for aggregate type: " + aggregateType);
        }
        return topic;
    }

}

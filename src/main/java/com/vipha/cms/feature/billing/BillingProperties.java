package com.vipha.cms.feature.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/**
 * @param currency        printed beside amounts on receipts
 * @param consultationFee charged once on every invoice generated from a visit
 */
@ConfigurationProperties(prefix = "cms.billing")
public record BillingProperties(
        @DefaultValue("THB") String currency,
        @DefaultValue("250.00") BigDecimal consultationFee
) {}

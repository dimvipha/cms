package com.vipha.cms.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.ZoneId;

/**
 * Clinic identity printed on documents (prescriptions, later invoices) and the
 * time zone used to turn stored instants into the clinic's local dates.
 */
@ConfigurationProperties(prefix = "cms.clinic")
public record ClinicProperties(
        @DefaultValue("CMS Clinic") String name,
        @DefaultValue("") String address,
        @DefaultValue("") String phone,
        @DefaultValue("Asia/Bangkok") ZoneId timeZone
) {}

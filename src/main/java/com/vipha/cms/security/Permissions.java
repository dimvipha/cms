package com.vipha.cms.security;

/**
 * Permissions the services check themselves, beyond the URL rules in SecurityConfig.
 * A permission without "-all" covers only the caller's own data.
 */
public final class Permissions {
    public static final String PATIENT_READ_ALL      = "patient:read-all";
    public static final String PATIENT_UPDATE_ALL    = "patient:update-all";
    public static final String APPOINTMENT_READ_ALL  = "appointment:read-all";
    public static final String APPOINTMENT_WRITE_ALL = "appointment:write-all";
    public static final String DOCTOR_WRITE          = "doctor:write";
    public static final String VISIT_WRITE           = "visit:write";
    public static final String VISIT_OPEN            = "visit:open";

    private Permissions() {}
}

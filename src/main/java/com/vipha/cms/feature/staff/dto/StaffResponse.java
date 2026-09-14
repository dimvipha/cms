package com.vipha.cms.feature.staff.dto;

import com.vipha.cms.feature.staff.StaffType;

public record StaffResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        StaffType role,
        String temporaryPassword
) {}

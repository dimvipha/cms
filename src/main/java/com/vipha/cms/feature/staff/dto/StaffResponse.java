package com.vipha.cms.feature.staff.dto;

import com.vipha.cms.feature.staff.StaffType;

import java.util.Set;

public record StaffResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Set<StaffType> roles
) {}

package com.vipha.cms.feature.userprofile.dto;

public record UserProfileResponse(
        String id,
        String firstName,
        String lastName,
        String gender,
        String biography,
        String profilePicture,
        String facebookProfile,
        String telegramProfile,
        String phoneNumber
) {
}
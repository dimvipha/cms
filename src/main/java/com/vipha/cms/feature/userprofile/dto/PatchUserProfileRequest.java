package com.vipha.cms.feature.userprofile.dto;

public record PatchUserProfileRequest(
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
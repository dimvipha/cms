package com.vipha.cms.feature.userprofile;

import com.vipha.cms.feature.userprofile.dto.PatchUserProfileRequest;
import com.vipha.cms.feature.userprofile.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PatchMapping
    public UserProfileResponse updateUserProfile(@Valid @RequestBody PatchUserProfileRequest request) {
        return userProfileService.updateUserProfile(request);
    }

    @GetMapping
    public UserProfileResponse getUserProfile() {
        return userProfileService.getUserProfile();
    }

    @GetMapping("/all")
    public Page<UserProfileResponse> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return userProfileService.findAll(page, size);
    }
}

package com.vipha.cms.feature.userprofile;

import com.vipha.cms.feature.userprofile.dto.PatchUserProfileRequest;
import com.vipha.cms.feature.userprofile.dto.UserProfileResponse;

public interface UserProfileService {
    UserProfileResponse getUserProfile();
    UserProfileResponse  updateUserProfile(PatchUserProfileRequest request);
}

package com.vipha.cms.feature.userprofile;

import com.vipha.cms.feature.userprofile.dto.PatchUserProfileRequest;
import com.vipha.cms.feature.userprofile.dto.UserProfileResponse;
import org.springframework.data.domain.Page;

public interface UserProfileService {
    UserProfileResponse getUserProfile();
    UserProfileResponse  updateUserProfile(PatchUserProfileRequest request);
    Page<UserProfileResponse> findAll(int pageNumber, int pageSize);
}

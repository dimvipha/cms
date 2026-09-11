package com.vipha.cms.feature.userprofile;

import com.vipha.cms.feature.userprofile.dto.UserProfileResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfileResponse toUserProfileResponse(UserProfile userProfile,String firstName,String lastName,String phoneNumber);
}

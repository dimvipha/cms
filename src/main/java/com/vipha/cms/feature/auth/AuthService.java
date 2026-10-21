package com.vipha.cms.feature.auth;

import com.vipha.cms.feature.auth.dto.ChangePasswordRequest;
import com.vipha.cms.feature.auth.dto.ForgotPasswordRequest;
import com.vipha.cms.feature.auth.dto.LoginRequest;
import com.vipha.cms.feature.auth.dto.LogoutRequest;
import com.vipha.cms.feature.auth.dto.RefreshRequest;
import com.vipha.cms.feature.auth.dto.RegisterRequest;
import com.vipha.cms.feature.auth.dto.TokenResponse;

public interface AuthService {
    void register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refresh(RefreshRequest request);
    void logout(LogoutRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void changePassword(ChangePasswordRequest request);
}

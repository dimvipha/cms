package com.vipha.cms.feature.auth;

import com.vipha.cms.feature.auth.dto.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
}

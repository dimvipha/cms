package com.vipha.cms.feature.auth.dto;

/**
 * After a sign-in: {@code joined} when the caller was new to the clinic and became a patient
 * just now. Their token predates the role, so the frontend fetches fresh tokens.
 */
public record WelcomeResponse(boolean joined) {}

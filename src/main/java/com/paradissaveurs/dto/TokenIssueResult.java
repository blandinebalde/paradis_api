package com.paradissaveurs.dto;

import java.time.Instant;

public record TokenIssueResult(String token, String jti, Instant expiresAt) {}

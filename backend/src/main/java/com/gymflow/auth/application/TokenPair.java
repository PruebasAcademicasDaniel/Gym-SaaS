package com.gymflow.auth.application;

public record TokenPair(String accessToken, String refreshToken, long accessTokenExpiresInSeconds) {
}

package id.ac.ui.cs.advprog.yomu.auth.service;

public record OAuth2UserIdentity(
        String registrationId,
        String email,
        String preferredUsername,
        String displayName
) {
}

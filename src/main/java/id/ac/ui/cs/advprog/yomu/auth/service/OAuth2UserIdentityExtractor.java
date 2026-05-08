package id.ac.ui.cs.advprog.yomu.auth.service;

import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class OAuth2UserIdentityExtractor {

    public OAuth2UserIdentity extract(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oauth2User.getAttributes();

        String email = firstNonBlank(
                asString(attributes.get("email")),
                providerFallbackEmail(registrationId, attributes)
        );
        if (email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "OAuth2 user info does not contain an email"
            );
        }

        String preferredUsername = firstNonBlank(
                asString(attributes.get("preferred_username")),
                asString(attributes.get("login")),
                localPartOfEmail(email),
                registrationId + "_user"
        );

        String displayName = firstNonBlank(
                asString(attributes.get("name")),
                preferredUsername
        );

        return new OAuth2UserIdentity(
                registrationId,
                normalize(email).toLowerCase(),
                normalize(preferredUsername),
                normalize(displayName)
        );
    }

    private String providerFallbackEmail(String registrationId, Map<String, Object> attributes) {
        if ("github".equalsIgnoreCase(registrationId)) {
            String login = normalize(asString(attributes.get("login")));
            String id = normalize(asString(attributes.get("id")));
            String localPart = firstNonBlank(login, id, "github_user");
            localPart = sanitizeLocalPart(localPart);
            return localPart + "@users.noreply.github.com";
        }
        return "";
    }

    private String sanitizeLocalPart(String value) {
        return normalize(value)
                .replaceAll("[^A-Za-z0-9._-]", "-")
                .replaceAll("-{2,}", "-");
    }

    private String localPartOfEmail(String email) {
        String normalizedEmail = normalize(email);
        int atIndex = normalizedEmail.indexOf('@');
        if (atIndex <= 0) {
            return "";
        }
        return normalizedEmail.substring(0, atIndex);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            String normalized = normalize(candidate);
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

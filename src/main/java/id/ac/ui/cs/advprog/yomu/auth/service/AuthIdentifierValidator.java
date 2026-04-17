package id.ac.ui.cs.advprog.yomu.auth.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AuthIdentifierValidator {

    public enum IdentifierType {
        EMAIL,
        USERNAME,
        INVALID
    }

    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$";
    public static final String USERNAME_REGEX = "^[A-Za-z0-9._-]{3,40}$";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private static final Pattern USERNAME_PATTERN = Pattern.compile(USERNAME_REGEX);

    public IdentifierType classify(String rawIdentifier) {
        String identifier = normalize(rawIdentifier);
        if (identifier.isBlank()) {
            return IdentifierType.INVALID;
        }
        if (isValidEmail(identifier)) {
            return IdentifierType.EMAIL;
        }
        if (isValidUsername(identifier)) {
            return IdentifierType.USERNAME;
        }
        return IdentifierType.INVALID;
    }

    public boolean isValidEmail(String rawEmail) {
        String email = normalize(rawEmail);
        return !email.isBlank() && EMAIL_PATTERN.matcher(email).matches();
    }

    public boolean isValidUsername(String rawUsername) {
        String username = normalize(rawUsername);
        if (username.isBlank()) {
            return false;
        }
        if (isValidEmail(username)) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

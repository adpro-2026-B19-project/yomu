package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import id.ac.ui.cs.advprog.yomu.auth.model.PasswordStrength;
import java.util.List;

public interface AuthService {
    List<AuthUser> findAllUsers();

    long countUsers();

    RegistrationResult registerUser(RegisterRequest request);

    record RegisterRequest(String email, String username, String password) {}

    record RegisteredUserSummary(String username, String email, String hashedPassword) {}

    record RegistrationResult(
            boolean success,
            String errorCode,
            String errorMessage,
            RegisteredUserSummary registeredUser,
            PasswordStrength passwordStrength
    ) {
        public static RegistrationResult successResult(RegisteredUserSummary registeredUser, PasswordStrength passwordStrength) {
            return new RegistrationResult(true, null, null, registeredUser, passwordStrength);
        }

        public static RegistrationResult failureResult(String errorCode, String errorMessage, PasswordStrength passwordStrength) {
            return new RegistrationResult(false, errorCode, errorMessage, null, passwordStrength);
        }

        public static RegistrationResult failureResult(String errorCode, String errorMessage) {
            return failureResult(errorCode, errorMessage, null);
        }
    }
}

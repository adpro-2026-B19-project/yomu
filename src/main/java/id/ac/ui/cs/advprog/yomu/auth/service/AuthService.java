package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.PasswordStrength;

public interface AuthService {
    RegistrationResult registerUser(RegisterRequest request);

    LoginResult loginUser(LoginRequest request);

    record RegisterRequest(String email, String username, String password) {}

    record LoginRequest(String email, String password) {}

    record RegisteredUserSummary(String username, String email) {}

    record LoggedInUserSummary(String username, String email) {}

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

    record LoginResult(boolean success, String errorCode, String errorMessage, LoggedInUserSummary loggedInUser) {
        public static LoginResult successResult(LoggedInUserSummary loggedInUser) {
            return new LoginResult(true, null, null, loggedInUser);
        }

        public static LoginResult failureResult(String errorCode, String errorMessage) {
            return new LoginResult(false, errorCode, errorMessage, null);
        }
    }
}

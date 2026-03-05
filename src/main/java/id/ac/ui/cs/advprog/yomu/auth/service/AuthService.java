package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import java.util.List;

public interface AuthService {
    List<AuthUser> findAllUsers();

    long countUsers();

    RegistrationResult registerUser(RegisterRequest request);

    record RegisterRequest(String email, String username, String password) {}

    record RegistrationResult(boolean success, String errorCode, String errorMessage) {
        public static RegistrationResult successResult() {
            return new RegistrationResult(true, null, null);
        }

        public static RegistrationResult failureResult(String errorCode, String errorMessage) {
            return new RegistrationResult(false, errorCode, errorMessage);
        }
    }
}

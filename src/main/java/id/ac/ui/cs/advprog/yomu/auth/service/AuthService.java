package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.AuthUser;
import java.util.List;

public interface AuthService {
    List<AuthUser> findAllUsers();

    long countUsers();

    RegistrationResult registerUser(String username);

    record RegistrationResult(boolean success, String errorCode, String errorMessage) {
        public static RegistrationResult successResult() {
            return new RegistrationResult(true, null, null);
        }

        public static RegistrationResult failureResult(String errorCode, String errorMessage) {
            return new RegistrationResult(false, errorCode, errorMessage);
        }
    }
}

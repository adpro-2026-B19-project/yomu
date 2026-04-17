package id.ac.ui.cs.advprog.yomu.auth.controller;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RegistrationErrorFieldMapper {

    private static final Map<String, String> FIELD_BY_ERROR_CODE = Map.of(
            "registration_failed", "email",
            "duplicate_email", "email",
            "duplicate_username", "username",
            "required_email", "email",
            "invalid_email", "email",
            "nonexistent_email", "email",
            "required_username", "username",
            "invalid_username", "username",
            "required_password", "password",
            "weak_password", "password"
    );

    public String resolve(String errorCode) {
        return FIELD_BY_ERROR_CODE.getOrDefault(errorCode, "email");
    }
}

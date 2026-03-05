package id.ac.ui.cs.advprog.yomu.auth.controller;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RegistrationErrorFieldMapper {

    private static final Map<String, String> FIELD_BY_ERROR_CODE = Map.of(
            "registration_failed", "email",
            "required_email", "email",
            "nonexistent_email", "email",
            "required_username", "username",
            "required_password", "password",
            "weak_password", "password"
    );

    public String resolve(String errorCode) {
        return FIELD_BY_ERROR_CODE.getOrDefault(errorCode, "email");
    }
}

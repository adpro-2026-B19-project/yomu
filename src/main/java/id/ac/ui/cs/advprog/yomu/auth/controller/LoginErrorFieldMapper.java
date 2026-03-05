package id.ac.ui.cs.advprog.yomu.auth.controller;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LoginErrorFieldMapper {

    private static final Map<String, String> FIELD_BY_ERROR_CODE = Map.of(
            "required_email", "email",
            "required_password", "password",
            "invalid_credentials", "password"
    );

    public String resolve(String errorCode) {
        return FIELD_BY_ERROR_CODE.getOrDefault(errorCode, "email");
    }
}

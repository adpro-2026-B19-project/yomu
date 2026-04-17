package id.ac.ui.cs.advprog.yomu.template;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProfileUpdateErrorFieldMapper {

    private static final Map<String, String> FIELD_BY_ERROR_CODE = Map.of(
            "required_username", "username",
            "duplicate_username", "username"
    );

    public String resolve(String errorCode) {
        return FIELD_BY_ERROR_CODE.getOrDefault(errorCode, "username");
    }
}

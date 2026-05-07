package id.ac.ui.cs.advprog.yomu.auth.service;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class SecureUsernameSuggestionGenerator implements UsernameSuggestionGenerator {

    private static final String PREFIX = "reader-";
    private static final int RANDOM_BYTES_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateSuggestion() {
        byte[] randomBytes = new byte[RANDOM_BYTES_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return PREFIX + HexFormat.of().formatHex(randomBytes);
    }
}

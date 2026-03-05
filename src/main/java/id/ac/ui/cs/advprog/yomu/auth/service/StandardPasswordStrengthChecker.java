package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.PasswordStrength;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class StandardPasswordStrengthChecker implements PasswordStrengthChecker {

    private static final Pattern HAS_LOWERCASE = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_UPPERCASE = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SPECIAL = Pattern.compile(".*[^A-Za-z0-9].*");

    @Override
    public PasswordStrength assess(String password) {
        if (password == null) {
            return PasswordStrength.WEAK;
        }

        int score = 0;
        if (password.length() >= 8) {
            score++;
        }
        if (HAS_LOWERCASE.matcher(password).matches()) {
            score++;
        }
        if (HAS_UPPERCASE.matcher(password).matches()) {
            score++;
        }
        if (HAS_DIGIT.matcher(password).matches()) {
            score++;
        }
        if (HAS_SPECIAL.matcher(password).matches()) {
            score++;
        }
        if (password.length() >= 12) {
            score++;
        }

        if (score <= 3) {
            return PasswordStrength.WEAK;
        }
        if (score <= 5) {
            return PasswordStrength.MEDIUM;
        }
        return PasswordStrength.STRONG;
    }
}

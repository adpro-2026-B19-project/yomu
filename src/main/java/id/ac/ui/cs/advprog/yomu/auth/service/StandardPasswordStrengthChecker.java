package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.PasswordStrength;
import org.springframework.stereotype.Component;

@Component
public class StandardPasswordStrengthChecker implements PasswordStrengthChecker {

    @Override
    public PasswordStrength assess(String password) {
        if (password == null) {
            return PasswordStrength.WEAK;
        }

        boolean hasLowercase = false;
        boolean hasUppercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int i = 0; i < password.length(); i++) {
            char current = password.charAt(i);
            if (current >= 'a' && current <= 'z') {
                hasLowercase = true;
            } else if (current >= 'A' && current <= 'Z') {
                hasUppercase = true;
            } else if (current >= '0' && current <= '9') {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        int score = 0;
        if (password.length() >= 8) {
            score++;
        }
        if (hasLowercase) {
            score++;
        }
        if (hasUppercase) {
            score++;
        }
        if (hasDigit) {
            score++;
        }
        if (hasSpecial) {
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

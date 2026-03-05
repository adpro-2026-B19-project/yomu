package id.ac.ui.cs.advprog.yomu.auth.service;

import id.ac.ui.cs.advprog.yomu.auth.model.PasswordStrength;

public interface PasswordStrengthChecker {
    PasswordStrength assess(String password);
}

package id.ac.ui.cs.advprog.yomu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import id.ac.ui.cs.advprog.yomu.auth.model.PasswordStrength;
import org.junit.jupiter.api.Test;

class StandardPasswordStrengthCheckerTest {

    private final StandardPasswordStrengthChecker checker = new StandardPasswordStrengthChecker();

    @Test
    void assessShouldHandleSlashHeavyInputInLinearWay() {
        String slashHeavyPassword = "A1a" + "/".repeat(5000);

        PasswordStrength result = checker.assess(slashHeavyPassword);

        assertThat(result).isEqualTo(PasswordStrength.STRONG);
    }

    @Test
    void assessShouldClassifyWeakMediumAndStrong() {
        assertThat(checker.assess("abc")).isEqualTo(PasswordStrength.WEAK);
        assertThat(checker.assess("Abcdef12")).isEqualTo(PasswordStrength.MEDIUM);
        assertThat(checker.assess("Abcdef12!XYZ")).isEqualTo(PasswordStrength.STRONG);
    }
}

package id.ac.ui.cs.advprog.yomu.achievement.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AchievementCreateFormTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validForm_noViolations() {
        AchievementCreateForm form = new AchievementCreateForm("First Steps", "Complete your first reading");
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    void blankName_hasViolation() {
        AchievementCreateForm form = new AchievementCreateForm("", "Complete your first reading");
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void blankMilestone_hasViolation() {
        AchievementCreateForm form = new AchievementCreateForm("First Steps", "");
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("milestone"));
    }

    @Test
    void nameTooLong_hasViolation() {
        String longName = "A".repeat(101);
        AchievementCreateForm form = new AchievementCreateForm(longName, "Complete your first reading");
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void milestoneTooLong_hasViolation() {
        String longMilestone = "A".repeat(256);
        AchievementCreateForm form = new AchievementCreateForm("First Steps", longMilestone);
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("milestone"));
    }
}
package id.ac.ui.cs.advprog.yomu.achievement.dto;

import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
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
        AchievementCreateForm form = new AchievementCreateForm(
                "First Steps",
                "Complete your first reading",
                AchievementRequirementType.READING_COUNT,
                1
        );
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    void blankName_hasViolation() {
        AchievementCreateForm form = new AchievementCreateForm(
                "",
                "Complete your first reading",
                AchievementRequirementType.READING_COUNT,
                1
        );
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void blankMilestone_hasViolation() {
        AchievementCreateForm form = new AchievementCreateForm(
                "First Steps",
                "",
                AchievementRequirementType.READING_COUNT,
                1
        );
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("milestone"));
    }

    @Test
    void nameTooLong_hasViolation() {
        String longName = "A".repeat(101);
        AchievementCreateForm form = new AchievementCreateForm(
                longName,
                "Complete your first reading",
                AchievementRequirementType.READING_COUNT,
                1
        );
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void milestoneTooLong_hasViolation() {
        String longMilestone = "A".repeat(256);
        AchievementCreateForm form = new AchievementCreateForm(
                "First Steps",
                longMilestone,
                AchievementRequirementType.READING_COUNT,
                1
        );
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("milestone"));
    }

    @Test
    void invalidTargetValue_hasViolation() {
        AchievementCreateForm form = new AchievementCreateForm(
                "First Steps",
                "Complete your first reading",
                AchievementRequirementType.READING_COUNT,
                0
        );
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("targetValue"));
    }

    @Test
    void nullRequirementType_hasViolation() {
        AchievementCreateForm form = new AchievementCreateForm(
                "First Steps",
                "Complete your first reading",
                null,
                1
        );
        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("requirementType"));
    }

    @Test
    void noArgConstructor_createsEmptyForm() {
        AchievementCreateForm form = new AchievementCreateForm();
        assertThat(form.getName()).isNull();
        assertThat(form.getMilestone()).isNull();
        assertThat(form.getRequirementType()).isNull();
        assertThat(form.getTargetValue()).isEqualTo(0);
    }

    @Test
    void setName_updatesNameProperty() {
        AchievementCreateForm form = new AchievementCreateForm();
        form.setName("New Achievement");
        assertThat(form.getName()).isEqualTo("New Achievement");
    }

    @Test
    void setMilestone_updatesMilestoneProperty() {
        AchievementCreateForm form = new AchievementCreateForm();
        form.setMilestone("New milestone description");
        assertThat(form.getMilestone()).isEqualTo("New milestone description");
    }

    @Test
    void setRequirementType_updatesRequirementTypeProperty() {
        AchievementCreateForm form = new AchievementCreateForm();
        form.setRequirementType(AchievementRequirementType.TOTAL_SCORE);
        assertThat(form.getRequirementType()).isEqualTo(AchievementRequirementType.TOTAL_SCORE);
    }

    @Test
    void setTargetValue_updatesTargetValueProperty() {
        AchievementCreateForm form = new AchievementCreateForm();
        form.setTargetValue(10);
        assertThat(form.getTargetValue()).isEqualTo(10);
    }

    @Test
    void settersWithValidation_allowsValidValueThroughValidation() {
        AchievementCreateForm form = new AchievementCreateForm();
        form.setName("Achievement");
        form.setMilestone("Milestone");
        form.setRequirementType(AchievementRequirementType.READING_COUNT);
        form.setTargetValue(5);

        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).isEmpty();
    }

    @Test
    void settersWithBlankName_failsValidation() {
        AchievementCreateForm form = new AchievementCreateForm();
        form.setName("");
        form.setMilestone("Milestone");
        form.setRequirementType(AchievementRequirementType.READING_COUNT);
        form.setTargetValue(5);

        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("name"));
    }

    @Test
    void settersWithBlankMilestone_failsValidation() {
        AchievementCreateForm form = new AchievementCreateForm();
        form.setName("Achievement");
        form.setMilestone("");
        form.setRequirementType(AchievementRequirementType.READING_COUNT);
        form.setTargetValue(5);

        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("milestone"));
    }

    @Test
    void settersWithNegativeTargetValue_failsValidation() {
        AchievementCreateForm form = new AchievementCreateForm();
        form.setName("Achievement");
        form.setMilestone("Milestone");
        form.setRequirementType(AchievementRequirementType.READING_COUNT);
        form.setTargetValue(-1);

        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("targetValue"));
    }

    @Test
    void settersWithNullRequirementType_failsValidation() {
        AchievementCreateForm form = new AchievementCreateForm();
        form.setName("Achievement");
        form.setMilestone("Milestone");
        form.setRequirementType(null);
        form.setTargetValue(5);

        Set<ConstraintViolation<AchievementCreateForm>> violations = validator.validate(form);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("requirementType"));
    }
}

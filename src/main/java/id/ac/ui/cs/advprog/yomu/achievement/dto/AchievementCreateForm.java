package id.ac.ui.cs.advprog.yomu.achievement.dto;

import id.ac.ui.cs.advprog.yomu.achievement.model.AchievementRequirementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class AchievementCreateForm {

    @NotBlank(message = "Achievement name is required")
    @Size(max = 100, message = "Achievement name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Milestone description is required")
    @Size(max = 255, message = "Milestone must be at most 255 characters")
    private String milestone;

    @NotNull(message = "Requirement type is required")
    private AchievementRequirementType requirementType;

    @Positive(message = "Target value must be greater than zero")
    private int targetValue;

    public AchievementCreateForm() {}

    public AchievementCreateForm(
            String name,
            String milestone,
            AchievementRequirementType requirementType,
            int targetValue
    ) {
        this.name = name;
        this.milestone = milestone;
        this.requirementType = requirementType;
        this.targetValue = targetValue;
    }

    public String getName() {
        return name;
    }

    public String getMilestone() {
        return milestone;
    }

    public AchievementRequirementType getRequirementType() {
        return requirementType;
    }

    public int getTargetValue() {
        return targetValue;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMilestone(String milestone) {
        this.milestone = milestone;
    }

    public void setRequirementType(AchievementRequirementType requirementType) {
        this.requirementType = requirementType;
    }

    public void setTargetValue(int targetValue) {
        this.targetValue = targetValue;
    }
}

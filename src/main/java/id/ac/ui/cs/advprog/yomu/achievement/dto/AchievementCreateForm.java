package id.ac.ui.cs.advprog.yomu.achievement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AchievementCreateForm {

    @NotBlank(message = "Achievement name is required")
    @Size(max = 100, message = "Achievement name must be at most 100 characters")
    private String name;

    @NotBlank(message = "Milestone description is required")
    @Size(max = 255, message = "Milestone must be at most 255 characters")
    private String milestone;

    public AchievementCreateForm() {}

    public AchievementCreateForm(String name, String milestone) {
        this.name = name;
        this.milestone = milestone;
    }

    public String getName() {
        return name;
    }

    public String getMilestone() {
        return milestone;
    }
}
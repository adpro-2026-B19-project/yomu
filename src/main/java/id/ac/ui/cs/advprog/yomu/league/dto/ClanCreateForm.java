package id.ac.ui.cs.advprog.yomu.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClanCreateForm {

    @NotBlank(message = "Clan name is required")
    @Size(max = 60, message = "Clan name must be at most 60 characters")
    private String name;

    public ClanCreateForm() {
        this.name = "";
    }

    public ClanCreateForm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

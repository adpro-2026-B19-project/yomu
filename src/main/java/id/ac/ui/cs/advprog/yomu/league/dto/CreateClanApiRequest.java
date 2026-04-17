package id.ac.ui.cs.advprog.yomu.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClanApiRequest(
        @NotBlank(message = "Clan name is required")
        @Size(max = 60, message = "Clan name must be at most 60 characters")
        String name
) {
}

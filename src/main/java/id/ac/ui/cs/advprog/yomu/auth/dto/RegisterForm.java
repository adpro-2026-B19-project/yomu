package id.ac.ui.cs.advprog.yomu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterForm {

    @NotBlank(message = "Username is required")
    @Size(max = 40, message = "Username must be 40 characters or fewer")
    private String username;
}

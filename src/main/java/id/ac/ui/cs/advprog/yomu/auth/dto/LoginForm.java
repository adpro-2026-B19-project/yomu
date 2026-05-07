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
public class LoginForm {

    @NotBlank(message = "Email or username is required")
    @Size(max = 255, message = "Email or username must be 255 characters or fewer")
    private String identifier;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must be 72 characters or fewer")
    private String password;
}

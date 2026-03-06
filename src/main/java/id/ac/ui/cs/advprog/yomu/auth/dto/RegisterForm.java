package id.ac.ui.cs.advprog.yomu.auth.dto;

import jakarta.validation.constraints.Email;
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

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Size(max = 255, message = "Email must be 255 characters or fewer")
    private String email;

    @Size(max = 40, message = "Username must be 40 characters or fewer")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must be 72 characters or fewer")
    private String password;
}

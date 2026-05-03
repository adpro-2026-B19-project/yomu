package id.ac.ui.cs.advprog.yomu.template.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateForm {

    @Size(max = 40, message = "Username must be 40 characters or fewer")
    private String username;

    @Size(max = 80, message = "Display name must be 80 characters or fewer")
    private String displayName;

    @Positive(message = "Phone number must be a positive number")
    private Long phoneNumber;
}

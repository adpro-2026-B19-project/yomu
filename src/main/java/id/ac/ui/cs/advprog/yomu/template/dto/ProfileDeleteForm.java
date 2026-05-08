package id.ac.ui.cs.advprog.yomu.template.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDeleteForm {

    @NotBlank(message = "Password is required")
    private String password;
}

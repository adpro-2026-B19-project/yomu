package id.ac.ui.cs.advprog.yomu.achievement.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "achievements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Achievement name cannot be blank")
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank(message = "Milestone description cannot be blank")
    @Column(nullable = false)
    private String milestone;
}
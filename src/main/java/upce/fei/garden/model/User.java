package upce.fei.garden.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import upce.fei.garden.model.enums.UserRole;

/**
 * Abstraktní základní třída pro všechny uživatele systému.
 * Obsahuje společné údaje pro registraci a přihlášení.
 */

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private UserRole role;
    @Column(nullable = false, unique = true)
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    @CreationTimestamp
    private LocalDateTime createdAt;

    private String avatarUrl; // TODO: implementovat nahrávání souborů
}

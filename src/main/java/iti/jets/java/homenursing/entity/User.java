package iti.jets.java.homenursing.entity;


import iti.jets.java.homenursing.entity.enums.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "phone_number")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    private String profileImageUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Profile> profiles = new java.util.ArrayList<>();
}

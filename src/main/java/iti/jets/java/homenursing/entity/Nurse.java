package iti.jets.java.homenursing.entity;

import iti.jets.java.homenursing.entity.enums.VerificationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "nurses")
public class Nurse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "national_id", unique = true, length = 100)
    private String nationalId;

    @Column(name = "license_number", unique = true, length = 100)
    private String licenseNumber;

    @Column(name = "national_id_front_url", columnDefinition = "TEXT")
    private String nationalIdFrontUrl;

    @Column(name = "national_id_back_url", columnDefinition = "TEXT")
    private String nationalIdBackUrl;

    @Column(name = "license_image_url", columnDefinition = "TEXT")
    private String licenseImageUrl;

    @Column(name = "professional_certificate_url", columnDefinition = "TEXT")
    private String professionalCertificateUrl;

    private String specialization;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Builder.Default
    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_reviews")
    private Integer totalReviews = 0;

    @Builder.Default
    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 50)
    private VerificationStatus verificationStatus = VerificationStatus.UNDER_REVIEW;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @OneToOne(mappedBy = "nurse", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private NurseRejectionDetail rejectionDetail;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

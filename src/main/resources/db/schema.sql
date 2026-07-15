

-- ========================================
-- 1. USERS
-- ========================================
CREATE TABLE users (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    phone_number VARCHAR(20) UNIQUE,
    email VARCHAR(255) UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20),
    --  account_type values later
    account_type VARCHAR(20) CHECK (account_type IN ('family', 'personal')),
    profile_image_url TEXT,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL
);

-- ========================================
-- USER TYPE TABLES
-- ========================================
-- One user (family/personal) can have many patient profiles
-- (self + family members under the same account)
CREATE TABLE patients (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    relationship VARCHAR(100),
    is_primary BOOLEAN DEFAULT FALSE,
    blood_type VARCHAR(5),
    height_cm DECIMAL(5, 2),
    weight_kg DECIMAL(5, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE nurses (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    license_number VARCHAR(100) UNIQUE NOT NULL,
    specialization VARCHAR(255),
    years_of_experience INT,
    hourly_rate DECIMAL(10, 2),
    bio TEXT,
    rating_avg DECIMAL(3, 2) DEFAULT 0,
    total_reviews INT DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    verification_status VARCHAR(50) DEFAULT 'pending'
        CHECK (verification_status IN ('pending', 'verified', 'rejected')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_nurses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ========================================
--  SERVICE CATALOG + NURSE SERVICES
-- ========================================
CREATE TABLE service_types (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    estimated_duration_minutes INT,
    base_price DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE nurse_services (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    nurse_id CHAR(36) NOT NULL,
    service_type_id CHAR(36) NOT NULL,
    custom_price DECIMAL(10, 2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (nurse_id, service_type_id),
    CONSTRAINT fk_nurse_services_nurse FOREIGN KEY (nurse_id) REFERENCES nurses(id) ON DELETE CASCADE,
    CONSTRAINT fk_nurse_services_service_type FOREIGN KEY (service_type_id) REFERENCES service_types(id) ON DELETE CASCADE
);

-- ========================================
-- HEALTH PROFILE TABLES
-- ========================================
CREATE TABLE medical_conditions (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

INSERT INTO medical_conditions (id, name) VALUES
    (UUID(), 'Diabetes'),
    (UUID(), 'Hypertension'),
    (UUID(), 'Heart Disease'),
    (UUID(), 'Asthma'),
    (UUID(), 'COPD'),
    (UUID(), 'Kidney Disease'),
    (UUID(), 'Liver Disease'),
    (UUID(), 'Cancer'),
    (UUID(), 'Epilepsy'),
    (UUID(), 'Thyroid Disease'),
    (UUID(), 'None');

CREATE TABLE patient_medical_conditions (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    patient_id CHAR(36) NOT NULL,
    medical_condition_id CHAR(36) NOT NULL,
    diagnosed_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (patient_id, medical_condition_id),
    CONSTRAINT fk_pmc_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_pmc_condition FOREIGN KEY (medical_condition_id) REFERENCES medical_conditions(id)
);

CREATE TABLE allergies (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) CHECK (type IN ('drug', 'food', 'other')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patient_allergies (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    patient_id CHAR(36) NOT NULL,
    allergy_id CHAR(36) NOT NULL,
    severity VARCHAR(50) CHECK (severity IN ('mild', 'moderate', 'severe')),
    reaction_description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (patient_id, allergy_id),
    CONSTRAINT fk_pa_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_pa_allergy FOREIGN KEY (allergy_id) REFERENCES allergies(id)
);

CREATE TABLE medications (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patient_medications (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    patient_id CHAR(36) NOT NULL,
    medication_id CHAR(36) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    start_date DATE,
    end_date DATE,
    is_current BOOLEAN DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pm_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_medication FOREIGN KEY (medication_id) REFERENCES medications(id)
);

CREATE TABLE medical_history (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    patient_id CHAR(36) NOT NULL,
    type VARCHAR(50) CHECK (type IN ('surgery', 'hospitalization', 'procedure', 'other')),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    date DATE,
    hospital_clinic_name VARCHAR(255),
    doctor_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mh_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE emergency_contacts (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    patient_id CHAR(36) NOT NULL,
    contact_name VARCHAR(255) NOT NULL,
    relationship VARCHAR(100),
    phone_number VARCHAR(20) NOT NULL,
    email VARCHAR(255),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ec_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE addresses (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    city VARCHAR(100) NOT NULL,
    area VARCHAR(255),
    street VARCHAR(255),
    building_number VARCHAR(50),
    apartment_number VARCHAR(50),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ========================================
--  SERVICE REQUEST & BOOKING TABLES
-- ========================================
CREATE TABLE service_requests (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    patient_id CHAR(36) NOT NULL,
    service_type_id CHAR(36),
    requested_nurse_id CHAR(36),
    service_description TEXT,
    preferred_date DATE,
    preferred_time TIME,
    duration_minutes INT,
    status VARCHAR(50) DEFAULT 'pending'
        CHECK (status IN (
            'pending',
            'ai_matching',
            'searching',
            'booking',
            'negotiating',
            'accepted',
            'rejected',
            'in_progress',
            'completed',
            'cancelled'
        )),
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sr_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_service_type FOREIGN KEY (service_type_id) REFERENCES service_types(id),
    CONSTRAINT fk_sr_nurse FOREIGN KEY (requested_nurse_id) REFERENCES nurses(id)
);

CREATE TABLE nurse_availability (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    nurse_id CHAR(36) NOT NULL,
    day_of_week INT CHECK (day_of_week BETWEEN 0 AND 6), -- 0=Sunday, 6=Saturday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_na_nurse FOREIGN KEY (nurse_id) REFERENCES nurses(id) ON DELETE CASCADE
);

CREATE TABLE bookings (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    service_request_id CHAR(36) NOT NULL,
    patient_id CHAR(36) NOT NULL,
    nurse_id CHAR(36) NOT NULL,
    scheduled_date DATE NOT NULL,
    scheduled_start_time TIME NOT NULL,
    scheduled_end_time TIME NOT NULL,
    status VARCHAR(50) DEFAULT 'pending'
        CHECK (status IN (
            'pending',
            'accepted',
            'rejected',
            'negotiating',
            'confirmed',
            'in_progress',
            'completed',
            'cancelled',
            'no_show'
        )),
    price DECIMAL(10, 2),
    negotiated_price DECIMAL(10, 2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_sr FOREIGN KEY (service_request_id) REFERENCES service_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_nurse FOREIGN KEY (nurse_id) REFERENCES nurses(id)
);

CREATE TABLE booking_negotiations (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    service_request_id CHAR(36) NOT NULL,
    booking_id CHAR(36),
    proposed_price DECIMAL(10, 2),
    proposed_time TIME,
    proposed_date DATE,
    message TEXT,
    status VARCHAR(50) DEFAULT 'pending'
        CHECK (status IN ('pending', 'accepted', 'rejected')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bn_service_request FOREIGN KEY (service_request_id) REFERENCES service_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_bn_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- TODO: revisit payment flow later
CREATE TABLE service_receipts (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    booking_id CHAR(36) NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(50) DEFAULT 'pending'
        CHECK (payment_status IN ('pending', 'paid', 'refunded', 'failed')),
    payment_method VARCHAR(50),
    payment_date TIMESTAMP NULL,
    service_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_receipts_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- ========================================
--  REVIEWS & RATINGS
-- ========================================
CREATE TABLE reviews_ratings (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    booking_id CHAR(36) NOT NULL,
    patient_id CHAR(36) NOT NULL,
    nurse_id CHAR(36) NOT NULL,
    rating INT CHECK (rating BETWEEN 1 AND 5),
    review_text TEXT,
    is_anonymous BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (booking_id),
    CONSTRAINT fk_rr_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_rr_nurse FOREIGN KEY (nurse_id) REFERENCES nurses(id)
);

-- ========================================
--  NOTIFICATIONS
-- ========================================
CREATE TABLE notifications (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    user_id CHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) CHECK (type IN ('booking', 'payment', 'system', 'message', 'reminder')),
    is_read BOOLEAN DEFAULT FALSE,
    related_entity_type VARCHAR(50),
    related_entity_id CHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ========================================
-- FAMILY MANAGEMENT
-- ========================================
CREATE TABLE family_groups (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    owner_id CHAR(36) NOT NULL,
    group_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fg_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE family_members (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    family_group_id CHAR(36) NOT NULL,
    user_id CHAR(36),
    patient_id CHAR(36),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(100),
    date_of_birth DATE,
    gender VARCHAR(20),
    phone_number VARCHAR(20),
    is_primary_member BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fm_group FOREIGN KEY (family_group_id) REFERENCES family_groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_fm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_fm_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE SET NULL
);

-- ========================================
-- INDEXES
-- ========================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone_number);
CREATE INDEX idx_patients_user_id ON patients(user_id);
CREATE INDEX idx_nurses_user_id ON nurses(user_id);
CREATE INDEX idx_nurses_available ON nurses(is_available);
CREATE INDEX idx_nurse_services_nurse ON nurse_services(nurse_id);
CREATE INDEX idx_nurse_services_service_type ON nurse_services(service_type_id);
CREATE INDEX idx_service_requests_patient ON service_requests(patient_id);
CREATE INDEX idx_service_requests_status ON service_requests(status);
CREATE INDEX idx_booking_negotiations_service_request ON booking_negotiations(service_request_id);
CREATE INDEX idx_bookings_nurse ON bookings(nurse_id);
CREATE INDEX idx_bookings_patient ON bookings(patient_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_date ON bookings(scheduled_date);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
CREATE INDEX idx_patient_medical_conditions ON patient_medical_conditions(patient_id);
CREATE INDEX idx_patient_allergies ON patient_allergies(patient_id);

-- ========================================
-- VIEWS
-- ========================================
CREATE VIEW v_available_nurses AS
SELECT
    n.id,
    n.user_id,
    u.first_name,
    u.last_name,
    u.phone_number,
    u.email,
    n.license_number,
    n.specialization,
    n.years_of_experience,
    n.hourly_rate,
    n.bio,
    n.rating_avg,
    n.total_reviews,
    n.is_available,
    n.verification_status
FROM nurses n
JOIN users u ON n.user_id = u.id
WHERE n.is_available = TRUE
  AND n.verification_status = 'verified'
  AND u.is_deleted = FALSE;

CREATE VIEW v_active_bookings AS
SELECT
    b.id,
    b.service_request_id,
    b.patient_id,
    b.nurse_id,
    p.first_name AS patient_first_name,
    p.last_name AS patient_last_name,
    n.first_name AS nurse_first_name,
    n.last_name AS nurse_last_name,
    b.scheduled_date,
    b.scheduled_start_time,
    b.scheduled_end_time,
    b.status,
    b.price,
    b.created_at
FROM bookings b
JOIN patients pat ON b.patient_id = pat.id
JOIN users p ON pat.user_id = p.id
JOIN nurses nur ON b.nurse_id = nur.id
JOIN users n ON nur.user_id = n.id
WHERE b.status NOT IN ('completed', 'cancelled');

-- ========================================
-- TRIGGERS (nurse rating auto-update)
-- ========================================
DELIMITER //

CREATE TRIGGER trigger_update_nurse_rating_insert
AFTER INSERT ON reviews_ratings
FOR EACH ROW
BEGIN
    IF NEW.rating IS NOT NULL THEN
        UPDATE nurses
        SET
            rating_avg = (
                SELECT AVG(rating)
                FROM reviews_ratings
                WHERE nurse_id = NEW.nurse_id
            ),
            total_reviews = (
                SELECT COUNT(*)
                FROM reviews_ratings
                WHERE nurse_id = NEW.nurse_id
            )
        WHERE id = NEW.nurse_id;
    END IF;
END//

CREATE TRIGGER trigger_update_nurse_rating_update
AFTER UPDATE ON reviews_ratings
FOR EACH ROW
BEGIN
    IF NEW.rating IS NOT NULL THEN
        UPDATE nurses
        SET
            rating_avg = (
                SELECT AVG(rating)
                FROM reviews_ratings
                WHERE nurse_id = NEW.nurse_id
            ),
            total_reviews = (
                SELECT COUNT(*)
                FROM reviews_ratings
                WHERE nurse_id = NEW.nurse_id
            )
        WHERE id = NEW.nurse_id;
    END IF;
END//

DELIMITER ;

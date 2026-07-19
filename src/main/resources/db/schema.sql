
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ========================================
-- USERS
-- ========================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    phone_number VARCHAR(20) UNIQUE,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    date_of_birth DATE,
    gender VARCHAR(20)
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),

    profile_image_url TEXT,

    is_deleted BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ
);

-- ========================================
-- PROFILES
-- ========================================

CREATE TABLE profiles (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    relationship UUID,

    first_name VARCHAR(100),

    last_name VARCHAR(100),

    date_of_birth DATE,

    gender VARCHAR(20)
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),

    blood_type VARCHAR(5),

    height_cm NUMERIC(5,2),

    weight_kg NUMERIC(5,2),

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_profiles_user
      FOREIGN KEY (user_id)
          REFERENCES users(id)
          ON DELETE CASCADE,

    CONSTRAINT fk_profiles_relationship
      FOREIGN KEY (relationship)
          REFERENCES profiles(id)
          ON DELETE SET NULL
);

-- ========================================
-- NURSES
-- ========================================

CREATE TABLE nurses (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,

    license_number VARCHAR(100) UNIQUE NOT NULL,

    specialization VARCHAR(255),

    years_of_experience INT,

    hourly_rate NUMERIC(10,2),

    bio TEXT,

    rating_avg NUMERIC(3,2) DEFAULT 0,

    total_reviews INT DEFAULT 0,

    is_available BOOLEAN DEFAULT TRUE,

    verification_status VARCHAR(50)
    DEFAULT 'PENDING'
    CHECK (
        verification_status IN (
                                'PENDING',
                                'VERIFIED',
                                'REJECTED'
            )
        ),

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_nurses_user
    FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- ========================================
-- SERVICE TYPES
-- ========================================

CREATE TABLE service_types (

   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

   name VARCHAR(255) NOT NULL,

   description TEXT,

   estimated_duration_minutes INT,

   base_price NUMERIC(10,2),

   created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- NURSE SERVICES
-- ========================================

CREATE TABLE nurse_services (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    nurse_id UUID NOT NULL,

    service_type_id UUID NOT NULL,

    custom_price NUMERIC(10,2),

    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (nurse_id, service_type_id),

    CONSTRAINT fk_nurse_services_nurse
        FOREIGN KEY (nurse_id)
            REFERENCES nurses(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_nurse_services_service_type
        FOREIGN KEY (service_type_id)
            REFERENCES service_types(id)
            ON DELETE CASCADE
);

-- ========================================
-- MEDICAL CONDITIONS
-- ========================================

CREATE TABLE medical_conditions (

        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

        name VARCHAR(255) UNIQUE NOT NULL,

        description TEXT
);

    INSERT INTO medical_conditions (id,name)
    VALUES
    (gen_random_uuid(),'Diabetes'),
    (gen_random_uuid(),'Hypertension'),
    (gen_random_uuid(),'Heart Disease'),
    (gen_random_uuid(),'Asthma'),
    (gen_random_uuid(),'COPD'),
    (gen_random_uuid(),'Kidney Disease'),
    (gen_random_uuid(),'Liver Disease'),
    (gen_random_uuid(),'Cancer'),
    (gen_random_uuid(),'Epilepsy'),
    (gen_random_uuid(),'Thyroid Disease'),
    (gen_random_uuid(),'None');

-- ========================================
-- PROFILE MEDICAL CONDITIONS
-- ========================================

CREATE TABLE profile_medical_conditions (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    profile_id UUID NOT NULL,

    medical_condition_id UUID NOT NULL,

    diagnosed_date DATE,

    notes TEXT,

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(profile_id,medical_condition_id),

    CONSTRAINT fk_pmc_profile
        FOREIGN KEY(profile_id)
            REFERENCES profiles(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_pmc_condition
        FOREIGN KEY(medical_condition_id)
            REFERENCES medical_conditions(id)
);

-- ========================================
-- ALLERGIES
-- ========================================

CREATE TABLE allergies (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(255) UNIQUE NOT NULL,

    type VARCHAR(50)
       CHECK(type IN ('DRUG', 'FOOD', 'OTHER')),

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- PROFILE ALLERGIES
-- ========================================

CREATE TABLE profile_allergies (

       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

       profile_id UUID NOT NULL,

       allergy_id UUID NOT NULL,

       severity VARCHAR(50)
           CHECK(severity IN ('MILD', 'MODERATE', 'SEVERE')),

       reaction_description TEXT,

       created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

       UNIQUE(profile_id,allergy_id),

       CONSTRAINT fk_pa_profile
           FOREIGN KEY(profile_id)
               REFERENCES profiles(id)
               ON DELETE CASCADE,

       CONSTRAINT fk_pa_allergy
           FOREIGN KEY(allergy_id)
               REFERENCES allergies(id)
);

-- ========================================
-- MEDICATIONS
-- ========================================

CREATE TABLE medications (

 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

 name VARCHAR(255) UNIQUE NOT NULL,

 description TEXT,

 created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- PROFILE MEDICATIONS
-- ========================================

CREATE TABLE profile_medications (

          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

          profile_id UUID NOT NULL,

          medication_id UUID NOT NULL,

          dosage VARCHAR(100),

          frequency VARCHAR(100),

          start_date DATE,

          end_date DATE,

          is_current BOOLEAN DEFAULT TRUE,

          notes TEXT,

          created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

          CONSTRAINT fk_pm_profile
              FOREIGN KEY(profile_id)
                  REFERENCES profiles(id)
                  ON DELETE CASCADE,

          CONSTRAINT fk_pm_medication
              FOREIGN KEY(medication_id)
                  REFERENCES medications(id)
);
-- ========================================
-- MEDICAL HISTORY
-- ========================================

CREATE TABLE medical_history (

     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

     profile_id UUID NOT NULL,

     type VARCHAR(50)
         CHECK (
             type IN (
                      'SURGERY',
                      'HOSPITALIZATION',
                      'PROCEDURE',
                      'OTHER'
                 )
             ),

     title VARCHAR(255) NOT NULL,

     description TEXT,

     date DATE,

     hospital_clinic_name VARCHAR(255),

     doctor_name VARCHAR(255),

     created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_mh_profile
         FOREIGN KEY (profile_id)
             REFERENCES profiles(id)
             ON DELETE CASCADE
);

-- ========================================
-- EMERGENCY CONTACTS
-- ========================================

CREATE TABLE emergency_contacts (

        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

        profile_id UUID NOT NULL,

        contact_name VARCHAR(255) NOT NULL,

        relationship VARCHAR(100),

        phone_number VARCHAR(20) NOT NULL,

        email VARCHAR(255),

        is_primary BOOLEAN DEFAULT FALSE,

        created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT fk_ec_profile
            FOREIGN KEY(profile_id)
                REFERENCES profiles(id)
                ON DELETE CASCADE
);

-- ========================================
-- ADDRESSES
-- ========================================

CREATE TABLE addresses (

id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

user_id UUID NOT NULL,

city VARCHAR(100) NOT NULL,

area VARCHAR(255),

street VARCHAR(255),

building_number VARCHAR(50),

apartment_number VARCHAR(50),

latitude NUMERIC(10,8),

longitude NUMERIC(11,8),

created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_addresses_user
   FOREIGN KEY(user_id)
       REFERENCES users(id)
       ON DELETE CASCADE
);

-- ========================================
-- SERVICE REQUESTS
-- ========================================

CREATE TABLE service_requests (

      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

      profile_id UUID NOT NULL,

      service_type_id UUID,

      requested_nurse_id UUID,

      service_description TEXT,

      preferred_date DATE,

      preferred_time TIME,

      duration_minutes INT,

      status VARCHAR(50)
                          DEFAULT 'PENDING'
          CHECK (
              status IN (
                         'PENDING',
                         'AI_MATCHING',
                         'SEARCHING',
                         'BOOKING',
                         'NEGOTIATING',
                         'ACCEPTED',
                         'REJECTED',
                         'IN_PROGRESS',
                         'COMPLETED',
                         'CANCELLED'
                  )
              ),

      latitude NUMERIC(10,8),

      longitude NUMERIC(11,8),

      created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

      updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

      CONSTRAINT fk_sr_profile
          FOREIGN KEY(profile_id)
              REFERENCES profiles(id)
              ON DELETE CASCADE,

      CONSTRAINT fk_sr_service_type
          FOREIGN KEY(service_type_id)
              REFERENCES service_types(id),

      CONSTRAINT fk_sr_nurse
          FOREIGN KEY(requested_nurse_id)
              REFERENCES nurses(id)
);

-- ========================================
-- NURSE AVAILABILITY
-- ========================================

CREATE TABLE nurse_availability (

        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

        nurse_id UUID NOT NULL,

        day_of_week INT
            CHECK(day_of_week BETWEEN 0 AND 6),

        start_time TIME NOT NULL,

        end_time TIME NOT NULL,

        is_available BOOLEAN DEFAULT TRUE,

        created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

        CONSTRAINT fk_na_nurse
            FOREIGN KEY(nurse_id)
                REFERENCES nurses(id)
                ON DELETE CASCADE
);

-- ========================================
-- BOOKINGS
-- ========================================

CREATE TABLE bookings (

id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

service_request_id UUID NOT NULL,

profile_id UUID NOT NULL,

nurse_id UUID NOT NULL,

scheduled_date DATE NOT NULL,

scheduled_start_time TIME NOT NULL,

scheduled_end_time TIME NOT NULL,

status VARCHAR(50) DEFAULT 'PENDING'
  CHECK (
      status IN (
                 'PENDING',
                 'ACCEPTED',
                 'REJECTED',
                 'NEGOTIATING',
                 'CONFIRMED',
                 'IN_PROGRESS',
                 'COMPLETED',
                 'CANCELLED',
                 'NO_SHOW'
          )
      ),

price NUMERIC(10,2),

negotiated_price NUMERIC(10,2),

notes TEXT,

created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_bookings_sr
  FOREIGN KEY(service_request_id)
      REFERENCES service_requests(id)
      ON DELETE CASCADE,

CONSTRAINT fk_bookings_profile
  FOREIGN KEY(profile_id)
      REFERENCES profiles(id)
      ON DELETE CASCADE,

CONSTRAINT fk_bookings_nurse
  FOREIGN KEY(nurse_id)
      REFERENCES nurses(id)
);

-- ========================================
-- BOOKING NEGOTIATIONS
-- ========================================

CREATE TABLE booking_negotiations (

          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

          service_request_id UUID NOT NULL,

          booking_id UUID,

          proposed_price NUMERIC(10,2),

          proposed_time TIME,

          proposed_date DATE,

          message TEXT,

          status VARCHAR(50)  DEFAULT 'PENDING'
              CHECK (
                  status IN (
                             'PENDING',
                             'ACCEPTED',
                             'REJECTED'
                      )
                  ),

          created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

          CONSTRAINT fk_bn_service_request
              FOREIGN KEY(service_request_id)
                  REFERENCES service_requests(id)
                  ON DELETE CASCADE,

          CONSTRAINT fk_bn_booking
              FOREIGN KEY(booking_id)
                  REFERENCES bookings(id)
                  ON DELETE CASCADE
);

-- ========================================
-- SERVICE RECEIPTS
-- ========================================

CREATE TABLE service_receipts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id UUID NOT NULL,
  total_amount NUMERIC(10,2) NOT NULL,

  payment_status VARCHAR(50)
                      DEFAULT 'PENDING'
      CHECK (payment_status IN ('PENDING', 'PAID', 'REFUNDED', 'FAILED')),

  payment_method VARCHAR(50),
  payment_date TIMESTAMPTZ,
  service_details TEXT,

  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_receipts_booking
      FOREIGN KEY (booking_id)
          REFERENCES bookings(id)
          ON DELETE CASCADE
);

-- ========================================
-- REVIEWS & RATINGS
-- ========================================

CREATE TABLE reviews_ratings (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

     booking_id UUID NOT NULL UNIQUE,
     profile_id UUID NOT NULL,
     nurse_id UUID NOT NULL,

     rating INT CHECK (rating BETWEEN 1 AND 5),

     review_text TEXT,
     is_anonymous BOOLEAN DEFAULT FALSE,

     created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_rr_booking
         FOREIGN KEY (booking_id)
             REFERENCES bookings(id)
             ON DELETE CASCADE,

     CONSTRAINT fk_rr_profile
         FOREIGN KEY (profile_id)
             REFERENCES profiles(id),

     CONSTRAINT fk_rr_nurse
         FOREIGN KEY (nurse_id)
             REFERENCES nurses(id)
);

-- ========================================
-- NOTIFICATIONS
-- ========================================

CREATE TABLE notifications (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

   user_id UUID NOT NULL,

   title VARCHAR(255) NOT NULL,
   message TEXT NOT NULL,

   type VARCHAR(50)
       CHECK (type IN ('BOOKING', 'PAYMENT', 'SYSTEM', 'MESSAGE', 'REMINDER')),

   is_read BOOLEAN DEFAULT FALSE,

   related_entity_type VARCHAR(50),
   related_entity_id UUID,

   created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT fk_notifications_user
       FOREIGN KEY (user_id)
           REFERENCES users(id)
           ON DELETE CASCADE
);

-- ========================================
-- INDEXES
-- ========================================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone_number);

CREATE INDEX idx_profiles_user_id ON profiles(user_id);
CREATE INDEX idx_profiles_relationship ON profiles(relationship);

CREATE INDEX idx_nurses_user_id ON nurses(user_id);
CREATE INDEX idx_nurses_available ON nurses(is_available);

CREATE INDEX idx_nurse_services_nurse
    ON nurse_services(nurse_id);

CREATE INDEX idx_nurse_services_service_type
    ON nurse_services(service_type_id);

CREATE INDEX idx_service_requests_profile
    ON service_requests(profile_id);

CREATE INDEX idx_service_requests_status
    ON service_requests(status);

CREATE INDEX idx_booking_negotiations_service_request
    ON booking_negotiations(service_request_id);

CREATE INDEX idx_bookings_nurse
    ON bookings(nurse_id);

CREATE INDEX idx_bookings_profile
    ON bookings(profile_id);

CREATE INDEX idx_bookings_status
    ON bookings(status);

CREATE INDEX idx_bookings_date
    ON bookings(scheduled_date);

CREATE INDEX idx_notifications_user
    ON notifications(user_id, is_read);

CREATE INDEX idx_profile_medical_conditions
    ON profile_medical_conditions(profile_id);

CREATE INDEX idx_profile_allergies
    ON profile_allergies(profile_id);

-- ========================================
-- VIEW : AVAILABLE NURSES
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
         JOIN users u
              ON u.id = n.user_id
WHERE
    n.is_available = TRUE
  AND n.verification_status = 'VERIFIED'
  AND u.is_deleted = FALSE;

-- ========================================
-- VIEW : ACTIVE BOOKINGS
-- ========================================

CREATE VIEW v_active_bookings AS
SELECT
    b.id,
    b.service_request_id,
    b.profile_id,
    b.nurse_id,

    p.first_name AS profile_first_name,
    p.last_name  AS profile_last_name,

    nu.first_name AS nurse_first_name,
    nu.last_name  AS nurse_last_name,

    b.scheduled_date,
    b.scheduled_start_time,
    b.scheduled_end_time,

    b.status,
    b.price,
    b.created_at

FROM bookings b
         JOIN profiles prof
              ON prof.id = b.profile_id
         JOIN users p
              ON p.id = pat.user_id
         JOIN nurses n
              ON n.id = b.nurse_id
         JOIN users nu
              ON nu.id = n.user_id

WHERE b.status NOT IN ('COMPLETED', 'CANCELLED');

-- ========================================
-- FUNCTION : UPDATE updated_at
-- ========================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS
$$
BEGIN
    NEW.updated_at := CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_nurses_updated_at
    BEFORE UPDATE ON nurses
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_service_requests_updated_at
    BEFORE UPDATE ON service_requests
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ========================================
-- FUNCTION : UPDATE NURSE RATING
-- ========================================

CREATE OR REPLACE FUNCTION update_nurse_rating()
RETURNS TRIGGER AS
$$
BEGIN
UPDATE nurses
SET
    rating_avg = (
        SELECT COALESCE(AVG(rating), 0)
        FROM reviews_ratings
        WHERE nurse_id = NEW.nurse_id
    ),
    total_reviews = (
        SELECT COUNT(*)
        FROM reviews_ratings
        WHERE nurse_id = NEW.nurse_id
    )
WHERE id = NEW.nurse_id;

RETURN NEW;
END;
$$
LANGUAGE plpgsql;

CREATE TRIGGER trg_rating_insert
    AFTER INSERT ON reviews_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_nurse_rating();

CREATE TRIGGER trg_rating_update
    AFTER UPDATE ON reviews_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_nurse_rating();

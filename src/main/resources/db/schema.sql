
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ==========================================================
-- USERS
-- ==========================================================

CREATE TABLE users (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

phone_number VARCHAR(20) UNIQUE,
email VARCHAR(255) UNIQUE,

first_name VARCHAR(100) NOT NULL,
last_name VARCHAR(100) NOT NULL,

date_of_birth DATE,

gender VARCHAR(20)
   CHECK (gender IN ('MALE', 'FEMALE')),

profile_image_url TEXT,

last_login_at TIMESTAMPTZ,

is_deleted BOOLEAN DEFAULT FALSE,

created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================================
-- PROFILES
-- ==========================================================

CREATE TABLE profiles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  user_id UUID NOT NULL,

  relationship VARCHAR(100),

  first_name VARCHAR(100),
  last_name VARCHAR(100),

  date_of_birth DATE,

  gender VARCHAR(20)
      CHECK (gender IN ('MALE', 'FEMALE')),

-- =========================
-- Basic Health Information
-- =========================

  blood_type VARCHAR(5),

  height NUMERIC(5,2),
  weight NUMERIC(5,2),

-- =========================
-- Mobility
-- =========================

  mobility_status VARCHAR(100),
  mobility_notes TEXT,

-- =========================
-- Medical History Summary
-- =========================

  previous_surgeries TEXT,
  previous_hospitalizations TEXT,

  is_primary BOOLEAN DEFAULT FALSE,

  is_deleted BOOLEAN DEFAULT FALSE,

  created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_profiles_user
      FOREIGN KEY (user_id)
          REFERENCES users(id)
          ON DELETE CASCADE
);

-- ==========================================================
-- NURSES
-- ==========================================================

CREATE TABLE nurses (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

user_id UUID NOT NULL,

-- =========================
-- Identity Documents
-- =========================

national_id VARCHAR(100) UNIQUE,

national_id_front_url TEXT,
national_id_back_url TEXT,

license_image_url TEXT,
professional_certificate_url TEXT,

-- =========================
-- Professional Information
-- =========================

specialization VARCHAR(255),

years_of_experience INT,

hourly_rate NUMERIC(10,2),

bio TEXT,

-- =========================
-- Rating
-- =========================

rating_avg NUMERIC(3,2) DEFAULT 0,
total_reviews INT DEFAULT 0,

-- =========================
-- Availability
-- =========================

is_available BOOLEAN DEFAULT TRUE,

verification_status VARCHAR(50)
DEFAULT 'UNDER_REVIEW'
CHECK (
verification_status IN (
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED'
)
),

rejection_reason TEXT,

created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_nurses_user
    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


-- ==========================================================
-- SERVICE TYPES
-- ==========================================================

CREATE TABLE service_types (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

      name VARCHAR(255) NOT NULL,
      description TEXT,

      estimated_duration_minutes INT,
      base_price NUMERIC(10,2),

      created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================================
-- NURSE SERVICES
-- ==========================================================

CREATE TABLE nurse_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    nurse_id UUID NOT NULL,
    service_type_id UUID NOT NULL,

    custom_price NUMERIC(10,2),

    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_nurse_service
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

-- ==========================================================
-- MEDICAL CONDITIONS
-- ==========================================================

CREATE TABLE medical_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,

    created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO medical_conditions (name, description)
VALUES
    ('Diabetes', 'Chronic condition affecting blood sugar'),
    ('Hypertension', 'High blood pressure'),
    ('Heart Disease', 'Conditions affecting the heart'),
    ('Asthma', 'Chronic respiratory condition'),
    ('COPD', 'Chronic Obstructive Pulmonary Disease'),
    ('Kidney Disease', 'Decreased kidney function'),
    ('Liver Disease', 'Conditions affecting the liver'),
    ('Cancer', 'Malignant cell growth'),
    ('Epilepsy', 'Neurological disorder with seizures'),
    ('Thyroid Disease', 'Disorders of the thyroid gland'),
    ('None', 'No known medical conditions');

-- ==========================================================
-- PROFILE MEDICAL CONDITIONS
-- ==========================================================

CREATE TABLE profile_medical_conditions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    profile_id UUID NOT NULL,
    medical_condition_id UUID NOT NULL,

    created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_profile_medical_condition
        UNIQUE (profile_id, medical_condition_id),

    CONSTRAINT fk_pmc_profile
        FOREIGN KEY (profile_id)
            REFERENCES profiles(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_pmc_condition
        FOREIGN KEY (medical_condition_id)
            REFERENCES medical_conditions(id)
);

-- ==========================================================
-- ALLERGIES
-- ==========================================================

CREATE TABLE allergies (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

   name VARCHAR(255) UNIQUE NOT NULL,

   type VARCHAR(50)
       CHECK (type IN ('DRUG', 'FOOD', 'OTHER')),

   created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================================
-- PROFILE ALLERGIES
-- ==========================================================

CREATE TABLE profile_allergies (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

       profile_id UUID NOT NULL,
       allergy_id UUID NOT NULL,

       created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

       CONSTRAINT uq_profile_allergy
           UNIQUE (profile_id, allergy_id),

       CONSTRAINT fk_pa_profile
           FOREIGN KEY (profile_id)
               REFERENCES profiles(id)
               ON DELETE CASCADE,

       CONSTRAINT fk_pa_allergy
           FOREIGN KEY (allergy_id)
               REFERENCES allergies(id)
);

-- ==========================================================
-- MEDICATIONS
-- ==========================================================

    CREATE TABLE medications (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

     name VARCHAR(255) UNIQUE NOT NULL,

     created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP
    );

-- ==========================================================
-- PROFILE MEDICATIONS
-- ==========================================================

CREATE TABLE profile_medications (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

     profile_id UUID NOT NULL,
     medication_id UUID NOT NULL,

     created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_pm_profile
         FOREIGN KEY (profile_id)
             REFERENCES profiles(id)
             ON DELETE CASCADE,

     CONSTRAINT fk_pm_medication
         FOREIGN KEY (medication_id)
             REFERENCES medications(id)
);

-- ==========================================================
-- MEDICAL HISTORY
-- ==========================================================

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

     description TEXT,

     created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_mh_profile
         FOREIGN KEY (profile_id)
             REFERENCES profiles(id)
             ON DELETE CASCADE
);

-- ==========================================================
-- EMERGENCY CONTACTS
-- ==========================================================

CREATE TABLE emergency_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    profile_id UUID NOT NULL,

    contact_name VARCHAR(255) NOT NULL,

    relationship VARCHAR(100),

    phone_number VARCHAR(20) NOT NULL,

    created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ec_profile
        FOREIGN KEY (profile_id)
            REFERENCES profiles(id)
            ON DELETE CASCADE
);

-- ==========================================================
-- ADDRESSES
-- ==========================================================

CREATE TABLE addresses (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

   profile_id UUID NOT NULL,

   country VARCHAR(100),
   city VARCHAR(100) NOT NULL,

   area VARCHAR(255),
   street VARCHAR(255),

   building_number VARCHAR(50),
   apartment_number VARCHAR(50),

  latitude NUMERIC(10,8),
  longitude NUMERIC(11,8),

  created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_addresses_profile
      FOREIGN KEY (profile_id)
          REFERENCES profiles(id)
          ON DELETE CASCADE
);


-- ==========================================================
-- SERVICE REQUESTS
-- ==========================================================

CREATE TABLE service_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

  profile_id UUID NOT NULL,
  service_type_id UUID,
  nurse_id UUID,

  service_description TEXT,

  preferred_date DATE,
  preferred_time TIME,

  duration_minutes INT,

  status VARCHAR(50)
                      DEFAULT 'PENDING'
      CHECK (
          status IN (
                     'PENDING',
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

  is_deleted BOOLEAN DEFAULT FALSE,

  created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_service_requests_profile
      FOREIGN KEY (profile_id)
          REFERENCES profiles(id)
          ON DELETE CASCADE,

  CONSTRAINT fk_service_requests_service_type
      FOREIGN KEY (service_type_id)
          REFERENCES service_types(id),

  CONSTRAINT fk_service_requests_nurse
      FOREIGN KEY (nurse_id)
          REFERENCES nurses(id)
);

-- ==========================================================
-- BOOKINGS
-- ==========================================================

CREATE TABLE bookings (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

service_request_id UUID NOT NULL,

profile_id UUID NOT NULL,
nurse_id UUID NOT NULL,

scheduled_date DATE ,

scheduled_start_time TIME,
scheduled_end_time TIME ,

status VARCHAR(50)
              DEFAULT 'PENDING'
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

is_deleted BOOLEAN DEFAULT FALSE,

created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_bookings_service_request
FOREIGN KEY (service_request_id)
  REFERENCES service_requests(id)
  ON DELETE CASCADE,

CONSTRAINT fk_bookings_profile
FOREIGN KEY (profile_id)
  REFERENCES profiles(id)
  ON DELETE CASCADE,

CONSTRAINT fk_bookings_nurse
FOREIGN KEY (nurse_id)
  REFERENCES nurses(id)
);

-- ==========================================================
-- NURSE OFFERS
-- ==========================================================

CREATE TABLE nurse_offers (
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

service_request_id UUID NOT NULL,
nurse_id UUID NOT NULL,

proposed_price NUMERIC(10,2),

proposed_date DATE,
proposed_time TIME,

message TEXT,

is_deleted BOOLEAN DEFAULT FALSE,

status VARCHAR(50)
                  DEFAULT 'PENDING'
  CHECK (
      status IN (
                 'PENDING',
                 'ACCEPTED',
                 'REJECTED'
          )
      ),

created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_nurse_offers_request
  FOREIGN KEY (service_request_id)
      REFERENCES service_requests(id)
      ON DELETE CASCADE,

CONSTRAINT fk_nurse_offers_nurse
  FOREIGN KEY (nurse_id)
      REFERENCES nurses(id)
      ON DELETE CASCADE
);

-- ==========================================================
-- CARECONNECT DATABASE
-- PostgreSQL Database Schema
-- Part 4 of 6
-- Receipts, Reviews & Notifications
-- ==========================================================

-- ==========================================================
-- SERVICE RECEIPTS
-- ==========================================================

CREATE TABLE service_receipts (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

      booking_id UUID NOT NULL,

      total_amount NUMERIC(10,2) NOT NULL,

      payment_status VARCHAR(50)
                          DEFAULT 'PENDING'
          CHECK (
              payment_status IN (
                                 'PENDING',
                                 'PAID',
                                 'REFUNDED',
                                 'FAILED'
                  )
              ),

      payment_method VARCHAR(50),
      payment_date TIMESTAMPTZ,

      service_details TEXT,

      is_deleted BOOLEAN DEFAULT FALSE,

      created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

      CONSTRAINT fk_service_receipts_booking
          FOREIGN KEY (booking_id)
              REFERENCES bookings(id)
              ON DELETE CASCADE
);

-- ==========================================================
-- REVIEWS & RATINGS
-- ==========================================================

CREATE TABLE reviews_ratings (
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

     booking_id UUID NOT NULL UNIQUE,
     profile_id UUID NOT NULL,
     nurse_id UUID NOT NULL,

     rating INT
         CHECK (rating BETWEEN 1 AND 5),

     review_text TEXT,

     is_anonymous BOOLEAN DEFAULT FALSE,

     created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_reviews_booking
         FOREIGN KEY (booking_id)
             REFERENCES bookings(id)
             ON DELETE CASCADE,

     CONSTRAINT fk_reviews_profile
         FOREIGN KEY (profile_id)
             REFERENCES profiles(id),

     CONSTRAINT fk_reviews_nurse
         FOREIGN KEY (nurse_id)
             REFERENCES nurses(id)
);

-- ==========================================================
-- NOTIFICATIONS
-- ==========================================================

CREATE TABLE notifications (
   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

   user_id UUID NOT NULL,

   title VARCHAR(255) NOT NULL,
   message TEXT NOT NULL,

   type VARCHAR(50)
       CHECK (
           type IN (
                    'BOOKING',
                    'PAYMENT',
                    'SYSTEM',
                    'MESSAGE',
                    'REMINDER'
               )
           ),

   is_read BOOLEAN DEFAULT FALSE,

   related_entity_type VARCHAR(50),
   related_entity_id UUID,

   created_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMPTZ NULL DEFAULT CURRENT_TIMESTAMP,

   CONSTRAINT fk_notifications_user
       FOREIGN KEY (user_id)
           REFERENCES users(id)
           ON DELETE CASCADE
);
-- ==========================================================
-- Views
-- ==========================================================

-- ==========================================================
-- AVAILABLE NURSES
-- ==========================================================

CREATE VIEW v_available_nurses AS
SELECT
    n.id,
    n.user_id,

    u.first_name,
    u.last_name,
    u.phone_number,
    u.email,

    n.national_id,
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
  AND n.verification_status = 'APPROVED'
  AND u.is_deleted = FALSE;

-- ==========================================================
-- ACTIVE BOOKINGS
-- ==========================================================

CREATE VIEW v_active_bookings AS
SELECT
    b.id,

    b.service_request_id,
    b.profile_id,
    b.nurse_id,

    patient.first_name AS patient_first_name,
    patient.last_name AS patient_last_name,

    nurse_user.first_name AS nurse_first_name,
    nurse_user.last_name AS nurse_last_name,

    b.scheduled_date,
    b.scheduled_start_time,
    b.scheduled_end_time,

    b.status,
    b.price,
    b.negotiated_price,

    b.created_at

FROM bookings b

         JOIN profiles p
              ON p.id = b.profile_id

         JOIN users patient
              ON patient.id = p.user_id

         JOIN nurses n
              ON n.id = b.nurse_id

         JOIN users nurse_user
              ON nurse_user.id = n.user_id

WHERE
    b.is_deleted = FALSE
  AND b.status NOT IN (
                       'COMPLETED',
                       'CANCELLED',
                       'NO_SHOW'
    );


-- ==========================================================
-- FUNCTIONS
-- ==========================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ==========================================================
-- UPDATE UPDATED_AT TRIGGERS
-- ==========================================================

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

CREATE TRIGGER trg_service_types_updated_at
    BEFORE UPDATE ON service_types
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_nurse_services_updated_at
    BEFORE UPDATE ON nurse_services
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_medical_conditions_updated_at
    BEFORE UPDATE ON medical_conditions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_profile_medical_conditions_updated_at
    BEFORE UPDATE ON profile_medical_conditions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_allergies_updated_at
    BEFORE UPDATE ON allergies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_profile_allergies_updated_at
    BEFORE UPDATE ON profile_allergies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_medications_updated_at
    BEFORE UPDATE ON medications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_profile_medications_updated_at
    BEFORE UPDATE ON profile_medications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_medical_history_updated_at
    BEFORE UPDATE ON medical_history
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_emergency_contacts_updated_at
    BEFORE UPDATE ON emergency_contacts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_addresses_updated_at
    BEFORE UPDATE ON addresses
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

CREATE TRIGGER trg_nurse_offers_updated_at
    BEFORE UPDATE ON nurse_offers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_service_receipts_updated_at
    BEFORE UPDATE ON service_receipts
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_reviews_updated_at
    BEFORE UPDATE ON reviews_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ==========================================================
-- UPDATE NURSE RATING
-- ==========================================================

CREATE OR REPLACE FUNCTION update_nurse_rating()
RETURNS TRIGGER AS $$
DECLARE
target_nurse_id UUID;
BEGIN

  IF TG_OP = 'DELETE' THEN
    target_nurse_id := OLD.nurse_id;
ELSE
    target_nurse_id := NEW.nurse_id;
END IF;

UPDATE nurses
SET
    rating_avg = (
        SELECT COALESCE(AVG(rating), 0)
        FROM reviews_ratings
        WHERE nurse_id = target_nurse_id
    ),

    total_reviews = (
        SELECT COUNT(*)
        FROM reviews_ratings
        WHERE nurse_id = target_nurse_id
    )

WHERE id = target_nurse_id;

IF TG_OP = 'DELETE' THEN
    RETURN OLD;
END IF;

RETURN NEW;

END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_reviews_insert
    AFTER INSERT ON reviews_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_nurse_rating();

CREATE TRIGGER trg_reviews_update
    AFTER UPDATE ON reviews_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_nurse_rating();

CREATE TRIGGER trg_reviews_delete
    AFTER DELETE ON reviews_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_nurse_rating();

-- ==========================================================
-- INDEXES
-- ==========================================================

CREATE INDEX idx_users_email
    ON users(email);

CREATE INDEX idx_users_phone
    ON users(phone_number);

CREATE INDEX idx_profiles_user
    ON profiles(user_id);

CREATE UNIQUE INDEX idx_profiles_primary
    ON profiles(user_id)
    WHERE is_primary = TRUE;

CREATE INDEX idx_nurses_user
    ON nurses(user_id);

CREATE INDEX idx_nurses_available
    ON nurses(is_available);

CREATE INDEX idx_nurse_services_nurse
    ON nurse_services(nurse_id);

CREATE INDEX idx_nurse_services_service_type
    ON nurse_services(service_type_id);

CREATE INDEX idx_profile_medical_conditions_profile
    ON profile_medical_conditions(profile_id);

CREATE INDEX idx_profile_allergies_profile
    ON profile_allergies(profile_id);

CREATE INDEX idx_profile_medications_profile
    ON profile_medications(profile_id);

CREATE INDEX idx_medical_history_profile
    ON medical_history(profile_id);

CREATE INDEX idx_emergency_contacts_profile
    ON emergency_contacts(profile_id);

CREATE INDEX idx_addresses_profile
    ON addresses(profile_id);

CREATE INDEX idx_service_requests_profile
    ON service_requests(profile_id);

CREATE INDEX idx_service_requests_status
    ON service_requests(status);

CREATE INDEX idx_service_requests_nurse
    ON service_requests(nurse_id);

CREATE INDEX idx_bookings_profile
    ON bookings(profile_id);

CREATE INDEX idx_bookings_nurse
    ON bookings(nurse_id);

CREATE INDEX idx_bookings_status
    ON bookings(status);

CREATE INDEX idx_bookings_date
    ON bookings(scheduled_date);

CREATE INDEX idx_nurse_offers_request
    ON nurse_offers(service_request_id);

CREATE INDEX idx_nurse_offers_nurse
    ON nurse_offers(nurse_id);

CREATE INDEX idx_reviews_nurse
    ON reviews_ratings(nurse_id);

CREATE INDEX idx_notifications_user
    ON notifications(user_id, is_read);
-- =====================================================
-- FinSentinel
-- V1: User and Role Management
-- PostgreSQL 17
-- =====================================================

-- 1. Application Users
CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    email VARCHAR(320) NOT NULL,
    phone_number VARCHAR(20),

    password_hash VARCHAR(255) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',

    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,

    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,

    last_login_at TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_user_status
        CHECK (status IN (
            'PENDING_VERIFICATION',
            'ACTIVE',
            'LOCKED',
            'SUSPENDED',
            'CLOSED'
        )),

    CONSTRAINT chk_failed_login_attempts
        CHECK (failed_login_attempts >= 0)
);

-- 2. Case-insensitive unique email
CREATE UNIQUE INDEX idx_users_email_unique
ON app_users (LOWER(email));

-- 3. Unique phone number
CREATE UNIQUE INDEX idx_users_phone_unique
ON app_users (phone_number)
WHERE phone_number IS NOT NULL;

-- 4. User status index
CREATE INDEX idx_users_status
ON app_users (status);

-- 5. User creation date index
CREATE INDEX idx_users_created_at
ON app_users (created_at DESC);

-- 6. Application Roles
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. User Role Mapping
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_user_roles
        PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE RESTRICT
);

-- 8. Default Roles
INSERT INTO roles (name, description)
VALUES
    ('CUSTOMER', 'Regular FinSentinel banking customer'),
    ('ADMIN', 'Platform administrator'),
    ('FRAUD_ANALYST', 'Fraud investigation and monitoring'),
    ('SUPPORT_AGENT', 'Customer support operations');
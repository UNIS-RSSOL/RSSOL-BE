ALTER TABLE bank_accounts
    DROP
        FOREIGN KEY bank_accounts_ibfk_1;

ALTER TABLE app_users
    DROP
        FOREIGN KEY fk_active_store;

ALTER TABLE user_refresh_tokens
    DROP
        FOREIGN KEY user_refresh_tokens_ibfk_1;

ALTER TABLE user_store
    DROP
        FOREIGN KEY user_store_ibfk_1;

CREATE TABLE attendance
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    user_store_id  BIGINT                NOT NULL,
    work_date      date                  NOT NULL,
    work_shift_id  BIGINT                NULL,
    is_checked_in  BIT(1)                NOT NULL,
    check_in_time  datetime              NULL,
    is_checked_out BIT(1)                NOT NULL,
    check_out_time datetime              NULL,
    status         VARCHAR(255)          NOT NULL,
    created_at     datetime              NULL,
    updated_at     datetime              NULL,
    CONSTRAINT pk_attendance PRIMARY KEY (id)
);

CREATE TABLE extra_shift_requests
(
    id                  BIGINT AUTO_INCREMENT NOT NULL,
    store_id            BIGINT                NOT NULL,
    owner_id            BIGINT                NOT NULL,
    base_shift_id       BIGINT                NULL,
    receiver_user_ids   VARCHAR(255)          NULL,
    start_datetime      datetime              NOT NULL,
    end_datetime        datetime              NOT NULL,
    headcount_requested INT                   NOT NULL,
    headcount_filled    INT                   NOT NULL,
    status              VARCHAR(16)           NOT NULL,
    note                TEXT                  NULL,
    created_at          datetime              NULL,
    updated_at          datetime              NULL,
    version             BIGINT                NULL,
    CONSTRAINT pk_extra_shift_requests PRIMARY KEY (id)
);

CREATE TABLE extra_shift_responses
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    extra_shift_request_id BIGINT                NOT NULL,
    candidate_id           BIGINT                NOT NULL,
    worker_action          VARCHAR(16)           NOT NULL,
    manager_approval       VARCHAR(16)           NOT NULL,
    created_at             datetime              NULL,
    CONSTRAINT pk_extra_shift_responses PRIMARY KEY (id)
);

CREATE TABLE minimum_wage
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    hourly_wage    INT                   NOT NULL,
    effective_from date                  NOT NULL,
    effective_to   date                  NULL,
    `description`  VARCHAR(255)          NULL,
    created_at     datetime              NULL,
    updated_at     datetime              NULL,
    CONSTRAINT pk_minimum_wage PRIMARY KEY (id)
);

CREATE TABLE notifications
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    user_id                BIGINT                NOT NULL,
    store_id               BIGINT                NULL,
    requester_id           BIGINT                NULL,
    target_type            VARCHAR(32)           NULL,
    target_id              BIGINT                NULL,
    shift_swap_request_id  BIGINT                NULL,
    extra_shift_request_id BIGINT                NULL,
    category               VARCHAR(16)           NOT NULL,
    type                   VARCHAR(64)           NOT NULL,
    message                TEXT                  NOT NULL,
    is_read                BIT(1)                NOT NULL,
    created_at             datetime              NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

CREATE TABLE schedule
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    store_id   BIGINT                NOT NULL,
    start_date date                  NOT NULL,
    end_date   date                  NOT NULL,
    CONSTRAINT pk_schedule PRIMARY KEY (id)
);

CREATE TABLE schedule_request
(
    id                     BIGINT AUTO_INCREMENT NOT NULL,
    store_id               BIGINT                NOT NULL,
    start_date             date                  NOT NULL,
    end_date               date                  NOT NULL,
    status                 VARCHAR(255)          NOT NULL,
    temporary_setting_key  VARCHAR(255)          NULL,
    candidate_schedule_key VARCHAR(255)          NULL,
    schedule_id            BIGINT                NULL,
    created_at             datetime              NULL,
    updated_at             datetime              NULL,
    CONSTRAINT pk_schedule_request PRIMARY KEY (id)
);

CREATE TABLE shift_swap_requests
(
    id                      BIGINT AUTO_INCREMENT NOT NULL,
    shift_id                BIGINT                NOT NULL,
    requester_id            BIGINT                NOT NULL,
    receiver_id             BIGINT                NOT NULL,
    reason                  VARCHAR(255)          NOT NULL,
    status                  VARCHAR(255)          NULL,
    manager_approval_status VARCHAR(255)          NULL,
    created_at              datetime              NULL,
    updated_at              datetime              NULL,
    CONSTRAINT pk_shift_swap_requests PRIMARY KEY (id)
);

CREATE TABLE store_setting
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    store_id         BIGINT                NOT NULL,
    open_time        time                  NOT NULL,
    close_time       time                  NOT NULL,
    use_segments     BIT(1)                NOT NULL,
    has_break_time   BIT(1)                NOT NULL,
    break_start_time time                  NULL,
    break_end_time   time                  NULL,
    created_at       datetime              NULL,
    updated_at       datetime              NULL,
    CONSTRAINT pk_store_setting PRIMARY KEY (id)
);

CREATE TABLE store_setting_segment
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    store_setting_id BIGINT                NOT NULL,
    start_time       time                  NOT NULL,
    end_time         time                  NOT NULL,
    CONSTRAINT pk_store_setting_segment PRIMARY KEY (id)
);

CREATE TABLE todo
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    store_id   BIGINT                NOT NULL,
    user_id    BIGINT                NOT NULL,
    date       date                  NOT NULL,
    todo_type  VARCHAR(20)           NOT NULL,
    content    VARCHAR(500)          NOT NULL,
    completed  BIT(1)                NOT NULL,
    created_at datetime              NULL,
    updated_at datetime              NULL,
    CONSTRAINT pk_todo PRIMARY KEY (id)
);

CREATE TABLE user
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    username           VARCHAR(255)          NULL,
    email              VARCHAR(255)          NULL,
    profile_image_url  VARCHAR(255)          NULL,
    provider           VARCHAR(255)          NULL,
    provider_id        VARCHAR(255)          NULL,
    active_store_id    BIGINT                NULL,
    kakao_access_token VARCHAR(500)          NULL,
    created_at         datetime              NULL,
    updated_at         datetime              NULL,
    CONSTRAINT pk_user PRIMARY KEY (id)
);

CREATE TABLE work_availability
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    user_store_id BIGINT                NOT NULL,
    day_of_week   VARCHAR(255)          NOT NULL,
    start_time    time                  NOT NULL,
    end_time      time                  NOT NULL,
    created_at    datetime              NULL,
    updated_at    datetime              NULL,
    CONSTRAINT pk_work_availability PRIMARY KEY (id)
);

CREATE TABLE work_shift
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    store_id       BIGINT                NOT NULL,
    user_store_id  BIGINT                NOT NULL,
    schedule_id    BIGINT                NULL,
    start_datetime datetime              NOT NULL,
    end_datetime   datetime              NOT NULL,
    shift_status   VARCHAR(255)          NULL,
    created_at     datetime              NULL,
    updated_at     datetime              NULL,
    CONSTRAINT pk_work_shift PRIMARY KEY (id)
);

ALTER TABLE user_store
    ADD hire_date date NULL;

ALTER TABLE user_store
    ADD hourly_wage INT NULL;

ALTER TABLE work_availability
    ADD CONSTRAINT uc_66319e0ac5749e78b3e0566ac UNIQUE (user_store_id);

ALTER TABLE user
    ADD CONSTRAINT uc_f8f375036e0d52e5f0480049f UNIQUE (provider, provider_id);

ALTER TABLE schedule_request
    ADD CONSTRAINT uc_schedule_request_schedule UNIQUE (schedule_id);

ALTER TABLE store_setting
    ADD CONSTRAINT uc_store_setting_store UNIQUE (store_id);

ALTER TABLE attendance
    ADD CONSTRAINT uk_attendance_day UNIQUE (user_store_id, work_date);

ALTER TABLE extra_shift_responses
    ADD CONSTRAINT uq_staffresp_request_candidate UNIQUE (extra_shift_request_id, candidate_id);

CREATE INDEX idx_extra_shift_requests_created ON extra_shift_requests (created_at);

CREATE INDEX idx_extra_shift_requests_store_status_created ON extra_shift_requests (store_id, status, created_at);

CREATE INDEX idx_notifications_target ON notifications (target_type, target_id);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at);

CREATE INDEX idx_staffresp_request_approval ON extra_shift_responses (extra_shift_request_id, manager_approval);

CREATE INDEX idx_todo_store_date ON todo (store_id, date);

CREATE INDEX idx_todo_type ON todo (todo_type);

CREATE INDEX idx_todo_user_date ON todo (user_id, date);

ALTER TABLE bank_accounts
    ADD CONSTRAINT FK_BANK_ACCOUNTS_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE extra_shift_requests
    ADD CONSTRAINT FK_EXTRA_SHIFT_REQUESTS_ON_OWNER FOREIGN KEY (owner_id) REFERENCES user_store (id);

ALTER TABLE extra_shift_requests
    ADD CONSTRAINT FK_EXTRA_SHIFT_REQUESTS_ON_STORE FOREIGN KEY (store_id) REFERENCES store (id);

ALTER TABLE extra_shift_responses
    ADD CONSTRAINT FK_EXTRA_SHIFT_RESPONSES_ON_CANDIDATE FOREIGN KEY (candidate_id) REFERENCES user_store (id);

CREATE INDEX idx_staffresp_candidate ON extra_shift_responses (candidate_id);

ALTER TABLE extra_shift_responses
    ADD CONSTRAINT FK_EXTRA_SHIFT_RESPONSES_ON_EXTRA_SHIFT_REQUEST FOREIGN KEY (extra_shift_request_id) REFERENCES extra_shift_requests (id);

CREATE INDEX idx_staffresp_request ON extra_shift_responses (extra_shift_request_id);

ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATIONS_ON_REQUESTER FOREIGN KEY (requester_id) REFERENCES user (id);

ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATIONS_ON_STORE FOREIGN KEY (store_id) REFERENCES store (id);

ALTER TABLE schedule
    ADD CONSTRAINT FK_SCHEDULE_ON_STORE FOREIGN KEY (store_id) REFERENCES store (id);

ALTER TABLE schedule_request
    ADD CONSTRAINT FK_SCHEDULE_REQUEST_ON_SCHEDULE FOREIGN KEY (schedule_id) REFERENCES schedule (id);

ALTER TABLE schedule_request
    ADD CONSTRAINT FK_SCHEDULE_REQUEST_ON_STORE FOREIGN KEY (store_id) REFERENCES store (id);

ALTER TABLE shift_swap_requests
    ADD CONSTRAINT FK_SHIFT_SWAP_REQUESTS_ON_RECEIVER FOREIGN KEY (receiver_id) REFERENCES user_store (id);

ALTER TABLE shift_swap_requests
    ADD CONSTRAINT FK_SHIFT_SWAP_REQUESTS_ON_REQUESTER FOREIGN KEY (requester_id) REFERENCES user_store (id);

ALTER TABLE shift_swap_requests
    ADD CONSTRAINT FK_SHIFT_SWAP_REQUESTS_ON_SHIFT FOREIGN KEY (shift_id) REFERENCES work_shift (id);

ALTER TABLE store_setting
    ADD CONSTRAINT FK_STORE_SETTING_ON_STORE FOREIGN KEY (store_id) REFERENCES store (id);

ALTER TABLE store_setting_segment
    ADD CONSTRAINT FK_STORE_SETTING_SEGMENT_ON_STORE_SETTING FOREIGN KEY (store_setting_id) REFERENCES store_setting (id);

ALTER TABLE todo
    ADD CONSTRAINT FK_TODO_ON_STORE FOREIGN KEY (store_id) REFERENCES store (id);

ALTER TABLE todo
    ADD CONSTRAINT FK_TODO_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE user_refresh_tokens
    ADD CONSTRAINT FK_USER_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE user_store
    ADD CONSTRAINT FK_USER_STORE_ON_USER FOREIGN KEY (user_id) REFERENCES user (id);

ALTER TABLE work_availability
    ADD CONSTRAINT FK_WORK_AVAILABILITY_ON_USER_STORE FOREIGN KEY (user_store_id) REFERENCES user_store (id);

ALTER TABLE work_shift
    ADD CONSTRAINT FK_WORK_SHIFT_ON_SCHEDULE FOREIGN KEY (schedule_id) REFERENCES schedule (id);

ALTER TABLE work_shift
    ADD CONSTRAINT FK_WORK_SHIFT_ON_STORE FOREIGN KEY (store_id) REFERENCES store (id);

ALTER TABLE work_shift
    ADD CONSTRAINT FK_WORK_SHIFT_ON_USER_STORE FOREIGN KEY (user_store_id) REFERENCES user_store (id);

DROP TABLE app_users;

ALTER TABLE bank_accounts
    DROP
        COLUMN created_at;

ALTER TABLE bank_accounts
    MODIFY account_number VARCHAR(255);

ALTER TABLE bank_accounts
    MODIFY account_number VARCHAR(255) NULL;

ALTER TABLE store
    MODIFY address VARCHAR(255);

ALTER TABLE bank
    MODIFY bank_code VARCHAR(255);

ALTER TABLE bank
    MODIFY bank_code VARCHAR(255) NULL;

ALTER TABLE bank
    MODIFY bank_name VARCHAR(255);

ALTER TABLE bank
    MODIFY bank_name VARCHAR(255) NULL;

ALTER TABLE store
    MODIFY business_registration_number VARCHAR(255);

ALTER TABLE user_store
    DROP
        COLUMN employment_status;

ALTER TABLE user_store
    DROP
        COLUMN position;

ALTER TABLE user_store
    ADD employment_status VARCHAR(255) NULL;

ALTER TABLE user_refresh_tokens
    MODIFY expires_at datetime NULL;

ALTER TABLE store
    MODIFY name VARCHAR(255) NULL;

ALTER TABLE store
    MODIFY phone_number VARCHAR(255);

ALTER TABLE user_store
    ADD position VARCHAR(255) NULL;

ALTER TABLE user_store
    MODIFY position VARCHAR(255) NULL;

ALTER TABLE store
    MODIFY store_code VARCHAR(255);

ALTER TABLE store
    MODIFY store_code VARCHAR(255) NULL;

ALTER TABLE user_store
    MODIFY store_id BIGINT NULL;

ALTER TABLE user_store
    MODIFY user_id BIGINT NULL;
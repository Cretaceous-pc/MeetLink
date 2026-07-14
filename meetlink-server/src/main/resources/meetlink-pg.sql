-- MeetLink PostgreSQL Schema
-- MySQL → PostgreSQL 转换

CREATE TABLE IF NOT EXISTS users
(
    id          VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(255) DEFAULT NULL,
    avatar      TEXT         DEFAULT NULL,
    email       VARCHAR(255) DEFAULT NULL,
    badge       TEXT         DEFAULT NULL,
    role        VARCHAR(50)  DEFAULT 'user',
    login_time  TIMESTAMP(3) DEFAULT NULL,
    create_time TIMESTAMP(3) NOT NULL,
    update_time TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS "group"
(
    id          VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL,
    avatar      TEXT         DEFAULT NULL,
    password    VARCHAR(255) NOT NULL DEFAULT '',
    description TEXT         DEFAULT NULL,
    max_members INT          DEFAULT 100,
    owner_id    VARCHAR(255) DEFAULT NULL,
    create_time TIMESTAMP(3) NOT NULL,
    update_time TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS chat_list
(
    id           VARCHAR(255) NOT NULL,
    user_id      VARCHAR(255) NOT NULL,
    target_id    VARCHAR(255) NOT NULL,
    target_info  TEXT         NOT NULL,
    unread_count INT          DEFAULT 0,
    last_message TEXT         DEFAULT NULL,
    type         VARCHAR(255) DEFAULT NULL,
    create_time  TIMESTAMP(3) NOT NULL,
    update_time  TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS message
(
    id            VARCHAR(255) NOT NULL,
    from_id       VARCHAR(255) NOT NULL,
    to_id         VARCHAR(255) NOT NULL,
    from_info     TEXT         NOT NULL,
    message       TEXT         DEFAULT NULL,
    reference_msg TEXT         DEFAULT NULL,
    at_user       TEXT         DEFAULT NULL,
    is_show_time  BOOLEAN      DEFAULT FALSE,
    type          VARCHAR(255) DEFAULT NULL,
    source        VARCHAR(255) DEFAULT NULL,
    create_time   TIMESTAMP(3) NOT NULL,
    update_time   TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_message_from_id_to_id ON message (from_id, to_id);

CREATE TABLE IF NOT EXISTS notify
(
    id             VARCHAR(255) NOT NULL,
    notify_title   VARCHAR(255) DEFAULT NULL,
    notify_content TEXT         DEFAULT NULL,
    type           VARCHAR(255) DEFAULT NULL,
    create_time    TIMESTAMP(3) NOT NULL,
    update_time    TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS room_member
(
    id          VARCHAR(255) NOT NULL,
    user_id     VARCHAR(255) NOT NULL,
    group_id    VARCHAR(255) NOT NULL,
    role        VARCHAR(50)  DEFAULT 'member',
    muted_until TIMESTAMP(3) DEFAULT NULL,
    join_time   TIMESTAMP(3) NOT NULL,
    create_time TIMESTAMP(3) NOT NULL,
    update_time TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rm_user_group ON room_member (user_id, group_id);

CREATE TABLE IF NOT EXISTS invite_code
(
    id             VARCHAR(255) NOT NULL,
    code           VARCHAR(255) NOT NULL,
    max_members    INT          DEFAULT 100,
    is_used        BOOLEAN      DEFAULT FALSE,
    used_by        VARCHAR(255) DEFAULT NULL,
    used_for_group VARCHAR(255) DEFAULT NULL,
    created_by     VARCHAR(255) DEFAULT NULL,
    create_time    TIMESTAMP(3) NOT NULL,
    update_time    TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ic_code ON invite_code (code);

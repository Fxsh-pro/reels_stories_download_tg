CREATE TABLE user_requests
(
    id           SERIAL PRIMARY KEY,
    message_id   INTEGER NOT NULL,
    chat_id      INTEGER NOT NULL,
    sender_id    INTEGER NOT NULL,
    first_name   TEXT,
    last_name    TEXT,
    user_name    TEXT,
    message_text TEXT,
    created_ts   BIGINT  NOT NULL
);
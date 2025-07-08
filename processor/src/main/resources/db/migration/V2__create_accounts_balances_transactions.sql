-- === users (неизменяемо) ===
CREATE TABLE IF NOT EXISTS users
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    username
    VARCHAR
(
    255
) NOT NULL UNIQUE,
    telegram_username VARCHAR
(
    255
),
    password VARCHAR
(
    255
) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked BOOLEAN NOT NULL DEFAULT TRUE,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT TRUE,
    balance_count_limit INTEGER DEFAULT 5,
    created_at BIGINT NOT NULL
    );

-- === user_roles (неизменяемо) ===
CREATE TABLE IF NOT EXISTS user_roles
(
    user_id
    BIGINT
    NOT
    NULL
    REFERENCES
    users
(
    id
) ON DELETE CASCADE,
    role VARCHAR
(
    100
) NOT NULL,
    PRIMARY KEY
(
    user_id,
    role
)
    );

-- === accounts (1:1 с users) ===
CREATE TABLE accounts
(
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT       NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    user_username          VARCHAR(255) NOT NULL,
    user_telegram_username VARCHAR(255),
    account_number         VARCHAR(16)  NOT NULL UNIQUE
);

-- === balances (N:1 с accounts) ===
CREATE TABLE account_balances
(
    id             BIGSERIAL PRIMARY KEY,
    account_id     BIGINT      NOT NULL REFERENCES accounts (id) ON DELETE CASCADE,
    balance        BIGINT      NOT NULL DEFAULT 0,
    is_primary     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at     BIGINT      NOT NULL,
    balance_number VARCHAR(20) NOT NULL UNIQUE
);

-- === transactions (двойная запись) ===
CREATE TABLE transactions
(
    id                      BIGSERIAL PRIMARY KEY,
    balance_id              BIGINT      NOT NULL
        REFERENCES account_balances (id) ON DELETE CASCADE,

    amount                  BIGINT      NOT NULL,
    transaction_type        VARCHAR(50) NOT NULL, -- TRANSFER_FROM | TRANSFER_TO ...
    transaction_status      VARCHAR(50) NOT NULL, -- CREATED | ...
    created_at              BIGINT      NOT NULL,

    receiver_balance_id     BIGINT      NOT NULL
        REFERENCES account_balances (id) ON DELETE CASCADE,

    receiver_transaction_id BIGINT                -- ссылка на «парную» запись
        REFERENCES transactions (id),

    UNIQUE (receiver_transaction_id)              -- чтобы не было дубликатов
);

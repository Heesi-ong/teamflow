DO $$
BEGIN
    IF EXISTS (
        SELECT lower(trim(email))
        FROM users
        GROUP BY lower(trim(email))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot normalize duplicate user email addresses';
    END IF;
END $$;

UPDATE users
SET email = lower(trim(email))
WHERE email <> lower(trim(email));

CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

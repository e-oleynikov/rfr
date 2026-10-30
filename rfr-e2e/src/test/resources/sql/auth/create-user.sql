INSERT INTO `rangiffler-auth`.`user`
    (username, password, enabled, account_non_expired, account_non_locked, credentials_non_expired)
VALUES (:username, :password, true, true, true, true)

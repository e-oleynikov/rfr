SELECT
    (SELECT COUNT(*) FROM `rangiffler-api`.`user` WHERE username = :username) +
    (SELECT COUNT(*) FROM `rangiffler-auth`.`user` WHERE username = :username)

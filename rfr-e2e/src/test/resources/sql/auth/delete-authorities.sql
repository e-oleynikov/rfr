DELETE FROM `rangiffler-auth`.`authority`
WHERE user_id IN (SELECT id FROM `rangiffler-auth`.`user` WHERE username = :username)

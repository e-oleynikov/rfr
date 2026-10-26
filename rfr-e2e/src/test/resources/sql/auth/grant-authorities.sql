INSERT INTO `rangiffler-auth`.`authority` (user_id, authority)
SELECT id, 'read' FROM `rangiffler-auth`.`user` WHERE username = :username
UNION ALL
SELECT id, 'write' FROM `rangiffler-auth`.`user` WHERE username = :username

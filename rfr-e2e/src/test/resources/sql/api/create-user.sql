INSERT INTO `rangiffler-api`.`user` (username, country_id)
SELECT :username, (SELECT id FROM `rangiffler-api`.`country` WHERE code = 'ru')
WHERE NOT EXISTS (SELECT 1 FROM `rangiffler-api`.`user` WHERE username = :username)

INSERT INTO `rangiffler-api`.`friendship`
    (id, requester_id, addressee_id, status, created_at, responded_at, version)
VALUES (
    UUID_TO_BIN(UUID(), true),
    (SELECT id FROM `rangiffler-api`.`user` WHERE username = :requesterUsername),
    (SELECT id FROM `rangiffler-api`.`user` WHERE username = :addresseeUsername),
    'PENDING', CURRENT_TIMESTAMP(6), NULL, 0
)

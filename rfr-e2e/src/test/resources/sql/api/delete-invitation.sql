DELETE friendship
FROM `rangiffler-api`.`friendship` friendship
JOIN `rangiffler-api`.`user` requester ON requester.id = friendship.requester_id
JOIN `rangiffler-api`.`user` addressee ON addressee.id = friendship.addressee_id
WHERE friendship.status = 'PENDING'
    AND requester.username = :requesterUsername AND addressee.username = :addresseeUsername

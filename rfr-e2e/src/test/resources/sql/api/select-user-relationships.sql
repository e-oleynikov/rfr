SELECT requester.username AS requester, addressee.username AS addressee,
    friendship.status, friendship.responded_at
FROM `rangiffler-api`.`friendship` friendship
JOIN `rangiffler-api`.`user` requester ON requester.id = friendship.requester_id
JOIN `rangiffler-api`.`user` addressee ON addressee.id = friendship.addressee_id
WHERE (requester.username = :username OR addressee.username = :username)
    AND friendship.status = :status
ORDER BY requester.username, addressee.username

SELECT EXISTS (
    SELECT 1 FROM `rangiffler-api`.`friendship` friendship
    JOIN `rangiffler-api`.`user` target_user
        ON (friendship.requester_id = target_user.id OR friendship.addressee_id = target_user.id)
    WHERE target_user.username = :username AND friendship.status = :status
)

package io.student.rangiffler.jupiter.fixture;

import io.student.rangiffler.data.TestUserRepository;

public enum Relation {
  FRIEND {
    @Override
    TestUser create(TestUserRepository repository, TestUser user, TestUser relatedUser) {
      repository.createFriendship(user, relatedUser);
      return user.withFriend(relatedUser);
    }
  },
  INCOME_INVITATION {
    @Override
    TestUser create(TestUserRepository repository, TestUser user, TestUser relatedUser) {
      repository.createInvitation(relatedUser, user);
      return user.withIncomingInvitationSender(relatedUser);
    }
  },
  OUTCOME_INVITATION {
    @Override
    TestUser create(TestUserRepository repository, TestUser user, TestUser relatedUser) {
      repository.createInvitation(user, relatedUser);
      return user.withOutcomeInvitations(relatedUser);
    }
  };

  abstract TestUser create(TestUserRepository repository, TestUser user, TestUser relatedUser);
}

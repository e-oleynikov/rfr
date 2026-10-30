package io.student.rangiffler.jupiter.fixture;

import io.student.rangiffler.data.TestUserRepository;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.utils.RandomDataUtils;

public record UserRequest(int friends, int incomingInvitations, int outcomeInvitations) {

  public UserRequest {
    if (friends < 0 || incomingInvitations < 0) {
      throw new IllegalArgumentException("User friends and incomingInvitations must be non-negative");
    }
    if (outcomeInvitations < 0) {
      throw new IllegalArgumentException("User outcomeInvitations must be non-negative");
    }
  }

  public UserRequest(int friends, int incomingInvitations) {
    this(friends, incomingInvitations, 0);
  }

  public UserRequest(User user) {
    this(user.friends(), user.incomingInvitations(), user.outcomeInvitations());
  }

  public static UserRequest from(User annotation) {
    return new UserRequest(annotation);
  }

  public UserFixture fixture() {
    UserFixture fixture = new SingeUserFixture(new TestUserRepository(), RandomDataUtils.randomUser());
    if (friends > 0) {
      fixture = new UserWithRelationsFixture(fixture, Relation.FRIEND, friends);
    }
    if (incomingInvitations > 0) {
      fixture = new UserWithRelationsFixture(fixture, Relation.INCOME_INVITATION, incomingInvitations);
    }
    if (outcomeInvitations > 0) {
      fixture = new UserWithRelationsFixture(fixture, Relation.OUTCOME_INVITATION, outcomeInvitations);
    }
    return fixture;
  }
}

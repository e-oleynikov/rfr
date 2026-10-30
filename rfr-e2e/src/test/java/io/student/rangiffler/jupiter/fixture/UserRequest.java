package io.student.rangiffler.jupiter.fixture;

import io.student.rangiffler.data.TestUserRepository;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.utils.RandomDataUtils;

public record UserRequest(int friends, int incomingInvitations) {

  public UserRequest {
    if (friends < 0 || incomingInvitations < 0) {
      throw new IllegalArgumentException("User friends and incomingInvitations must be non-negative");
    }
  }

  public static UserRequest from(User annotation) {
    return new UserRequest(annotation.friends(), annotation.incomingInvitations());
  }

  public UserFixture fixture() {
    UserFixture fixture = new SingeUserFixture(new TestUserRepository(), RandomDataUtils.randomUser());
    if (friends > 0) {
      fixture = new UserWithFriendFixture(fixture, friends);
    }
    if (incomingInvitations > 0) {
      fixture = new UserWithIncomeInvitations(fixture, incomingInvitations);
    }
    return fixture;
  }
}

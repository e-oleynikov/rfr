package io.student.rangiffler.jupiter.fixture;

public final class UserWithFriendFixture extends UserRelationshipFixture {

  public UserWithFriendFixture(UserFixture delegate, int friends) {
    super(delegate, friends);
  }

  @Override
  protected TestUser createRelationship(TestUser relatedUser) {
    repository.createFriendship(user(), relatedUser);
    return user().withFriend(relatedUser);
  }
}

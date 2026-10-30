package io.student.rangiffler.jupiter.fixture;

import io.student.rangiffler.data.TestUserRepository;

public final class SingeUserFixture implements UserFixture {

  private final TestUserRepository repository;
  private final TestUser user;
  private boolean cleanupRequired;

  public SingeUserFixture(TestUserRepository repository, TestUser user) {
    this.repository = repository;
    this.user = user;
  }

  @Override
  public void create() {
    if (cleanupRequired) {
      throw new IllegalStateException("The fixture user has already been created");
    }
    repository.verifyUserAbsent(user.username());
    // Enable cleanup before insertion so partially failed setup is covered as well.
    cleanupRequired = true;
    repository.create(user);
  }

  @Override
  public TestUser user() {
    return user;
  }

  @Override
  public void close() {
    if (cleanupRequired) {
      // The repository also removes friendships and invitations created during the test.
      repository.delete(user.username());
      cleanupRequired = false;
    }
  }
}

package io.student.rangiffler.jupiter.fixture;

import io.student.rangiffler.data.TestUserRepository;

public final class UserFixture implements AutoCloseable {

  private final TestUserRepository repository;
  private final TestUser user;
  private boolean cleanupRequired;

  public UserFixture(TestUserRepository repository, TestUser user) {
    this.repository = repository;
    this.user = user;
  }

  public void create() {
    if (cleanupRequired) {
      throw new IllegalStateException("The fixture user has already been created");
    }
    repository.verifyUserAbsent(user.username());
    // Enable cleanup before insertion so partially failed setup is covered as well.
    cleanupRequired = true;
    repository.create(user);
  }

  public TestUser user() {
    return user;
  }

  @Override
  public void close() {
    if (cleanupRequired) {
      repository.delete(user.username());
      cleanupRequired = false;
    }
  }
}

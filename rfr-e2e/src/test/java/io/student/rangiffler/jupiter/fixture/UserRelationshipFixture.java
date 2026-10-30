package io.student.rangiffler.jupiter.fixture;

import io.student.rangiffler.data.TestUserRepository;
import io.student.rangiffler.utils.RandomDataUtils;
import io.student.rangiffler.utils.TearDown;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

abstract class UserRelationshipFixture implements UserFixture {

  protected final TestUserRepository repository = new TestUserRepository();
  private final UserFixture delegate;
  private final int count;
  private final List<UserFixture> relatedUsers = new ArrayList<>();
  private TestUser user;
  private boolean creationAttempted;

  protected UserRelationshipFixture(UserFixture delegate, int count) {
    this.delegate = Objects.requireNonNull(delegate);
    if (count < 0) {
      throw new IllegalArgumentException("Relationship count must be non-negative");
    }
    this.count = count;
    this.user = delegate.user();
  }

  @Override
  public final void create() {
    if (creationAttempted) {
      throw new IllegalStateException("The fixture creation has already been attempted");
    }
    creationAttempted = true;
    delegate.create();
    user = delegate.user();
    if (count == 0) {
      return;
    }
    repository.createApiProfile(user());
    for (int index = 0; index < count; index++) {
      UserFixture related = new SingeUserFixture(repository, RandomDataUtils.randomUser());
      // Register before creation so close() also covers partially failed setup.
      relatedUsers.add(related);
      related.create();
      repository.createApiProfile(related.user());
      user = createRelationship(related.user());
    }
  }

  protected abstract TestUser createRelationship(TestUser relatedUser);

  @Override
  public final TestUser user() {
    return user;
  }

  @Override
  public final void close() {
    new TearDown()
        .withEach(relatedUsers.reversed(), related -> {
          related.close();
          relatedUsers.remove(related);
        })
        .with(delegate::close)
        .run();
  }
}

package io.student.rangiffler.jupiter.fixture;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public final class StaticUserFixture implements UserFixture {

  // Demonstration credentials only: these users are not created in any database.
  private static final BlockingQueue<TestUser> USERS = new ArrayBlockingQueue<>(3, true, List.of(
      new TestUser("static_test_user_1", "demo-only-1"),
      new TestUser("static_test_user_2", "demo-only-2"),
      new TestUser("static_test_user_3", "demo-only-3")));

  private final Duration timeout;
  private TestUser user;
  private boolean creationAttempted;

  public StaticUserFixture() {
    this(Duration.ofSeconds(5));
  }

  public StaticUserFixture(Duration timeout) {
    this.timeout = Objects.requireNonNull(timeout);
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("Static user timeout must be positive");
    }
  }

  @Override
  public synchronized void create() {
    if (creationAttempted) {
      throw new IllegalStateException("The fixture creation has already been attempted");
    }
    creationAttempted = true;
    try {
      user = USERS.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for a static test user", error);
    }
    if (user == null) {
      throw new IllegalStateException("No static test user became available within " + timeout
          + "; all 3 users are borrowed");
    }
  }

  @Override
  public synchronized TestUser user() {
    if (user == null) {
      throw new IllegalStateException("This fixture does not own a static test user");
    }
    return user;
  }

  @Override
  public synchronized void close() {
    if (user != null) {
      if (!USERS.offer(user)) {
        throw new IllegalStateException("Cannot return static test user: pool is already full");
      }
      user = null;
    }
  }
}

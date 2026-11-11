package io.student.rangiffler.jupiter.fixture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class StaticUserFixtureTest {

  @Test
  void waitingThreadReceivesReturnedUser() throws Exception {
    try (var first = new StaticUserFixture();
         var second = new StaticUserFixture();
         var third = new StaticUserFixture();
         var executor = Executors.newSingleThreadExecutor()) {
      first.create();
      second.create();
      third.create();
      TestUser released = first.user();
      CountDownLatch waiting = new CountDownLatch(1);
      var result = executor.submit(() -> {
        try (var fixture = new StaticUserFixture()) {
          waiting.countDown();
          fixture.create();
          return fixture.user();
        }
      });
      assertTrue(waiting.await(1, TimeUnit.SECONDS));
      assertThrows(TimeoutException.class, () -> result.get(25, TimeUnit.MILLISECONDS));
      first.close();
      assertSame(released, result.get(2, TimeUnit.SECONDS));
    }
  }

  @Test
  void exhaustionTimesOutAndOnlyBorrowedUsersAreReturned() {
    try (var first = new StaticUserFixture();
         var second = new StaticUserFixture();
         var third = new StaticUserFixture();
         var fourth = new StaticUserFixture(Duration.ofMillis(25))) {
      first.create();
      second.create();
      third.create();
      assertEquals(3, Set.of(first.user().username(), second.user().username(), third.user().username()).size());
      var error = assertThrows(IllegalStateException.class, fourth::create);
      assertTrue(error.getMessage().contains("all 3 users are borrowed"));
      fourth.close();
      try (var stillEmpty = new StaticUserFixture(Duration.ofMillis(25))) {
        assertThrows(IllegalStateException.class, stillEmpty::create);
      }

      TestUser released = first.user();
      first.close();
      first.close();
      try (var replacement = new StaticUserFixture()) {
        replacement.create();
        assertSame(released, replacement.user());
        try (var noDuplicate = new StaticUserFixture(Duration.ofMillis(25))) {
          assertThrows(IllegalStateException.class, noDuplicate::create);
        }
      }
    }
  }

  @Test
  void repeatedCreateCannotBorrowAnotherUser() {
    try (var fixture = new StaticUserFixture()) {
      assertThrows(IllegalStateException.class, fixture::user);
      fixture.create();
      TestUser borrowed = fixture.user();
      assertThrows(IllegalStateException.class, fixture::create);
      assertSame(borrowed, fixture.user());
    }
  }
}

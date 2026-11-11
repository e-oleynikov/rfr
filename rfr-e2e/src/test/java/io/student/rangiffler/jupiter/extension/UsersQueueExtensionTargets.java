package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.jupiter.annotation.StaticUser;
import io.student.rangiffler.jupiter.fixture.TestUser;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

final class UsersQueueExtensionTargets {

  static final String ENABLED = "rfr.users-queue.testkit.enabled";

  static boolean enabled(ExtensionContext context) {
    return context.getConfigurationParameter(ENABLED).map(Boolean::parseBoolean).orElse(false);
  }

  @Target(TYPE)
  @Retention(RUNTIME)
  @EnabledIf("io.student.rangiffler.jupiter.extension.UsersQueueExtensionTargets#enabled")
  @interface EngineTarget {
  }

  @EngineTarget
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  static class FourSequentialTests {
    @Test
    @Order(1)
    void first(@StaticUser TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
    }

    @Test
    @Order(2)
    void second(@StaticUser TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
    }

    @Test
    @Order(3)
    void third(@StaticUser TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
    }

    @Test
    @Order(4)
    void fourth(@StaticUser TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
    }
  }

  @EngineTarget
  static class FailedTest {
    @Test
    void failsAfterBorrowing(@StaticUser TestUser user, TestReporter reporter) {
      reporter.publishEntry("username", user.username());
      fail("Intentional pooled user target failure");
    }
  }

  @EngineTarget
  static class BorrowWholePool {
    @Test
    void borrowsThreeDistinctUsers(@StaticUser TestUser first, @StaticUser TestUser second,
                                  @StaticUser TestUser third, TestReporter reporter) {
      assertEquals(3, Set.of(first.username(), second.username(), third.username()).size());
      reporter.publishEntry("username", first.username());
      reporter.publishEntry("username", second.username());
      reporter.publishEntry("username", third.username());
    }
  }
}

package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Invitations;
import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.jupiter.fixture.UserFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

final class UserExtensionTargets {

  static final String ENABLED = "rfr.user-extension.testkit.enabled";

  private UserExtensionTargets() {
  }

  static boolean enabled(ExtensionContext context) {
    return context.getConfigurationParameter(ENABLED).map(Boolean::parseBoolean).orElse(false);
  }

  @Target(TYPE)
  @Retention(RUNTIME)
  @EnabledIf("io.student.rangiffler.jupiter.extension.UserExtensionTargets#enabled")
  @ExtendWith({UserExtension.class, LifecycleProbe.class})
  @interface EngineTarget {
  }

  abstract static class Lifecycle {
    @BeforeEach
    void beforeEach(TestReporter reporter) {
      reporter.publishEntry("phase", "beforeEach");
    }

    @AfterEach
    void afterEach(TestReporter reporter) {
      reporter.publishEntry("phase", "afterEach");
    }
  }

  @EngineTarget
  @DisplayName("Пользователь без друзей и заявок")
  static class DefaultUser extends Lifecycle {
    @Test
    @DisplayName("Получить пользователя с пустыми списками связей")
    void test(@User TestUser user, TestReporter reporter) {
      assertTrue(user.friends().isEmpty());
      assertTrue(user.incomingInvitationSenders().isEmpty());
      reporter.publishEntry("phase", "test");
    }
  }

  @EngineTarget
  @DisplayName("Пользователь с двумя друзьями и тремя входящими заявками")
  static class RelatedUser extends Lifecycle {
    @Test
    @DisplayName("Получить подготовленные списки друзей и отправителей заявок")
    void test(@User(friends = 2, incomingInvitations = 3) TestUser user, TestReporter reporter) {
      assertEquals(2, user.friends().size());
      assertEquals(3, user.incomingInvitationSenders().size());
      reporter.publishEntry("phase", "test");
    }
  }

  @EngineTarget
  @DisplayName("Пользователь с одним исходящим приглашением")
  static class OutgoingInvitation extends Lifecycle {
    @Test
    @DisplayName("Связь PENDING направлена от пользователя к приглашённому")
    void test(@User(outcomeInvitations = 1) TestUser user, TestReporter reporter) {
      assertEquals(1, user.outcomeInvitations().size());
      TestUser invitee = user.outcomeInvitations().getFirst();
      var client = Jdbc.getClient(Databases.AUTH);
      assertTrue(new Profile(client, invitee.username()).exist());
      var invitations = new Invitations(client, user.username()).list();
      assertEquals(1, invitations.size());
      var invitation = invitations.getFirst();
      assertEquals(user.username(), invitation.requester());
      assertEquals(invitee.username(), invitation.addressee());
      assertEquals("PENDING", invitation.status());
      assertFalse(invitation.responded());
      reporter.publishEntry("phase", "test");
    }
  }

  @EngineTarget
  @DisplayName("Отрицательное количество исходящих приглашений")
  static class NegativeOutgoingInvitations extends Lifecycle {
    @Test
    @DisplayName("Отклонить outcomeInvitations = -1 до создания пользователя")
    void test(@User(outcomeInvitations = -1) TestUser user, TestReporter reporter) {
      reporter.publishEntry("phase", "test");
      fail("Negative count must prevent the test body");
    }
  }

  @EngineTarget
  @DisplayName("Два независимых @User между встроенными параметрами JUnit")
  static class MultipleUsers extends Lifecycle {
    @Test
    @DisplayName("Получить разные фикстуры по индексам параметров")
    void test(TestReporter reporter, @User(friends = 1) TestUser first,
              TestInfo info, @User(incomingInvitations = 2) TestUser second) {
      assertNotEquals(first.username(), second.username());
      assertEquals(1, first.friends().size());
      assertTrue(first.incomingInvitationSenders().isEmpty());
      assertTrue(second.friends().isEmpty());
      assertEquals(2, second.incomingInvitationSenders().size());
      assertTrue(info.getTestMethod().isPresent());
      reporter.publishEntry("phase", "test");
    }
  }

  @EngineTarget
  @DisplayName("Падение тела теста после подготовки пользователя")
  static class FailedTest extends Lifecycle {
    @Test
    @DisplayName("Завершиться ожидаемой ошибкой после подготовки данных")
    void test(@User(friends = 1, incomingInvitations = 1) TestUser user, TestReporter reporter) {
      reporter.publishEntry("phase", "test");
      fail("Intentional target failure");
    }
  }

  @EngineTarget
  @DisplayName("Корректный первый @User и неверный тип второго параметра")
  static class PartialSetup extends Lifecycle {
    @Test
    @DisplayName("Не выполнять тело после ошибки второго параметра")
    void test(@User(friends = 1, incomingInvitations = 1) TestUser first,
              @User String invalid, TestReporter reporter) {
      reporter.publishEntry("phase", "test");
      fail("Invalid parameter must prevent the test body");
    }
  }

  @EngineTarget
  @DisplayName("Аннотация @User на параметре String")
  static class WrongType extends Lifecycle {
    @Test
    @DisplayName("Отклонить неподдерживаемый тип параметра")
    void test(@User String invalid, TestReporter reporter) {
      reporter.publishEntry("phase", "test");
      fail("Invalid parameter must prevent the test body");
    }
  }

  @EngineTarget
  @DisplayName("Отрицательное количество друзей")
  static class NegativeFriends extends Lifecycle {
    @Test
    @DisplayName("Отклонить friends = -1")
    void test(@User(friends = -1) TestUser user, TestReporter reporter) {
      reporter.publishEntry("phase", "test");
      fail("Negative count must prevent the test body");
    }
  }

  @EngineTarget
  @DisplayName("Отрицательное количество входящих заявок")
  static class NegativeInvitations extends Lifecycle {
    @Test
    @DisplayName("Отклонить incomingInvitations = -1")
    void test(@User(incomingInvitations = -1) TestUser user, TestReporter reporter) {
      reporter.publishEntry("phase", "test");
      fail("Negative count must prevent the test body");
    }
  }

  @EngineTarget
  @DisplayName("Параметр TestUser без аннотации @User")
  static class UnannotatedUser extends Lifecycle {
    @Test
    @DisplayName("Не разрешать параметр без аннотации")
    void test(TestUser user, TestReporter reporter) {
      reporter.publishEntry("phase", "test");
      fail("Unannotated TestUser must not be resolved");
    }
  }

  @EngineTarget
  @DisplayName("Тест без параметров @User")
  static class NoUser extends Lifecycle {
    @Test
    @DisplayName("Получить встроенный TestReporter без создания пользователя")
    void test(TestReporter reporter) {
      reporter.publishEntry("phase", "test");
    }
  }

  public static final class LifecycleProbe implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) {
      reportUsers(context, "prepared");
    }

    @Override
    public void afterEach(ExtensionContext context) {
      reportUsers(context, "beforeStoreClose");
    }

    private void reportUsers(ExtensionContext context, String phase) {
      TestReporter reporter = context::publishReportEntry;
      reporter.publishEntry("phase", phase);
      var store = context.getStore(ExtensionContext.Namespace.create(UserExtension.class));
      var client = Jdbc.getClient(Databases.AUTH);
      for (int index = 0; index < context.getRequiredTestMethod().getParameterCount(); index++) {
        UserFixture fixture = store.get(index, UserFixture.class);
        if (fixture == null) {
          continue;
        }
        TestUser user = fixture.user();
        reporter.publishEntry(Map.of(
            "phase", phase + "User", "parameter", Integer.toString(index), "username", user.username(),
            "friends", user.friends().stream().map(TestUser::username).collect(Collectors.joining(",", "[", "]")),
            "senders", user.incomingInvitationSenders().stream().map(TestUser::username)
                .collect(Collectors.joining(",", "[", "]")),
            "invitees", user.outcomeInvitations().stream().map(TestUser::username)
                .collect(Collectors.joining(",", "[", "]"))));
        assertTrue(new AuthAccount(client, user.username()).exist(), "User must still exist at " + phase);
        assertEquals(user.friends().stream().map(TestUser::username).sorted().toList(),
            new Friendships(client, user.username()).list().stream()
                .map(link -> link.otherUser(user.username())).sorted().toList());
        assertEquals(user.incomingInvitationSenders().stream().map(TestUser::username).sorted().toList(),
            new Invitations(client, user.username()).list().stream()
                .filter(link -> link.addressee().equals(user.username()))
                .map(link -> link.requester()).sorted().toList());
        assertEquals(user.outcomeInvitations().stream().map(TestUser::username).sorted().toList(),
            new Invitations(client, user.username()).list().stream()
                .filter(link -> link.requester().equals(user.username()))
                .map(link -> link.addressee()).sorted().toList());
      }
    }
  }
}

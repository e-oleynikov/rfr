package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Authorities;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Invitations;
import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.data.db.Profile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@DisplayName("UserExtension: подготовка пользователей и автоматическая очистка")
class UserExtensionTest {

  @DisplayName("Пользователи готовы до теста и удаляются после закрытия Store")
  @ParameterizedTest(name = "{0}")
  @MethodSource("successfulScenarios")
  void preparesUsersBeforeTestAndCleansAfterStoreCloses(Class<?> target, TestReporter reporter) {
    EngineExecutionResults results = execute(target);
    assertOutcome(results, true);
    var entries = entries(results, reporter);
    assertEquals(List.of("prepared", "beforeEach", "test", "afterEach", "beforeStoreClose"), phases(entries));
    var prepared = entries.stream().filter(entry -> "preparedUser".equals(entry.get("phase"))).toList();
    int expectedUsers = target == UserExtensionTargets.MultipleUsers.class ? 2 : 1;
    assertEquals(expectedUsers, prepared.size());
    assertEquals(expectedUsers, prepared.stream().map(entry -> entry.get("username")).distinct().count());
    assertEquals(expectedUsers, entries.stream()
        .filter(entry -> "beforeStoreCloseUser".equals(entry.get("phase"))).count());
    if (target == UserExtensionTargets.MultipleUsers.class) {
      assertEquals(List.of("1", "3"), prepared.stream().map(entry -> entry.get("parameter")).toList());
    }
    int expectedAccounts = target == UserExtensionTargets.RelatedUser.class ? 6
        : target == UserExtensionTargets.MultipleUsers.class ? 5 : 1;
    assertCleaned(entries, expectedAccounts, reporter);
  }

  @Test
  @DisplayName("Падение теста не мешает удалению пользователя, друзей и заявок")
  void cleansUsersWhenTestFails(TestReporter reporter) {
    var results = execute(UserExtensionTargets.FailedTest.class);
    Throwable failure = assertFailure(results, AssertionError.class);
    assertEquals("Intentional target failure", failure.getMessage());
    var entries = entries(results, reporter);
    assertEquals(List.of("prepared", "beforeEach", "test", "afterEach", "beforeStoreClose"), phases(entries));
    assertCleaned(entries, 3, reporter);
  }

  @Test
  @DisplayName("Ошибка второго параметра не оставляет данные первого пользователя")
  void cleansFirstFixtureWhenSecondParameterSetupFails(TestReporter reporter) {
    var results = execute(UserExtensionTargets.PartialSetup.class);
    Throwable failure = assertFailure(results, ExtensionConfigurationException.class);
    assertEquals("@User is supported only for TestUser parameters", failure.getMessage());
    var entries = entries(results, reporter);
    assertEquals(List.of("beforeStoreClose"), phases(entries));
    assertCleaned(entries, 3, reporter);
  }

  @DisplayName("Некорректная аннотация отклоняется до выполнения тела теста")
  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidScenarios")
  void rejectsInvalidAnnotationBeforeTestBody(Class<?> target, TestReporter reporter) {
    var results = execute(target);
    Class<? extends Throwable> expected = target == UserExtensionTargets.WrongType.class
        ? ExtensionConfigurationException.class : IllegalArgumentException.class;
    Throwable failure = assertFailure(results, expected);
    assertEquals(target == UserExtensionTargets.WrongType.class
        ? "@User is supported only for TestUser parameters"
        : "User friends and incomingInvitations must be non-negative", failure.getMessage());
    var entries = entries(results, reporter);
    assertEquals(List.of("beforeStoreClose"), phases(entries));
    assertCleaned(entries, 0, reporter);
  }

  @Test
  @DisplayName("Параметр TestUser без @User не разрешается расширением")
  void doesNotResolveUnannotatedUser(TestReporter reporter) {
    var results = execute(UserExtensionTargets.UnannotatedUser.class);
    Throwable failure = assertFailure(results, ParameterResolutionException.class);
    assertTrue(failure.getMessage().contains("No ParameterResolver registered"));
    var entries = entries(results, reporter);
    assertEquals(List.of("prepared", "beforeEach", "afterEach", "beforeStoreClose"), phases(entries));
    assertCleaned(entries, 0, reporter);
  }

  @Test
  @DisplayName("Тест без @User продолжает получать встроенный TestReporter")
  void leavesOtherParameterResolversWorking(TestReporter reporter) {
    var results = execute(UserExtensionTargets.NoUser.class);
    assertOutcome(results, true);
    var entries = entries(results, reporter);
    assertEquals(List.of("prepared", "beforeEach", "test", "afterEach", "beforeStoreClose"), phases(entries));
    assertCleaned(entries, 0, reporter);
  }

  @Test
  @DisplayName("Классы-мишени отключены без параметра запуска EngineTestKit")
  void targetsAreDisabledWithoutEngineTestKitParameter(TestReporter reporter) {
    allTargets().forEach(target -> {
      var results = EngineTestKit.engine("junit-jupiter").selectors(selectClass(target)).execute();
      assertEquals(1, results.containerEvents().skipped().count());
      assertEquals(0, results.testEvents().started().count());
      assertEquals(0, results.allEvents().reportingEntryPublished().count());
      assertEquals(0, results.allEvents().failed().count());
      reporter.publishEntry("disabledTarget", target.getSimpleName());
    });
  }

  private EngineExecutionResults execute(Class<?> target) {
    return EngineTestKit.engine("junit-jupiter").selectors(selectClass(target))
        .configurationParameter(UserExtensionTargets.ENABLED, "true")
        .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
        .configurationParameter("junit.jupiter.extensions.store.close.autocloseable.enabled", "true")
        .execute();
  }

  private void assertOutcome(EngineExecutionResults results, boolean success) {
    assertEquals(1, results.testEvents().started().count());
    assertEquals(1, results.testEvents().finished().count());
    assertEquals(success ? 1 : 0, results.testEvents().succeeded().count(),
        () -> results.allEvents().failed().map(event -> event.getRequiredPayload(TestExecutionResult.class)
            .getThrowable().map(Throwable::toString).orElse("Unknown failure")).toList().toString());
    assertEquals(success ? 0 : 1, results.testEvents().failed().count());
    assertEquals(0, results.containerEvents().failed().count());
  }

  private Throwable assertFailure(EngineExecutionResults results, Class<? extends Throwable> type) {
    assertOutcome(results, false);
    Throwable failure = results.testEvents().failed().list().getFirst()
        .getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow();
    assertInstanceOf(type, failure);
    return failure;
  }

  private List<Map<String, String>> entries(EngineExecutionResults results, TestReporter reporter) {
    var entries = results.allEvents().reportingEntryPublished()
        .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs()).toList();
    entries.forEach(reporter::publishEntry);
    return entries;
  }

  private List<String> phases(List<Map<String, String>> entries) {
    return entries.stream().filter(entry -> !entry.containsKey("username"))
        .map(entry -> entry.get("phase")).toList();
  }

  private void assertCleaned(List<Map<String, String>> entries, int expectedAccounts, TestReporter reporter) {
    var usernames = entries.stream().filter(entry -> entry.containsKey("username"))
        .flatMap(entry -> Stream.of(entry.get("username"), entry.get("friends"), entry.get("senders")))
        .flatMap(value -> Arrays.stream(value.split("[\\[\\],]")))
        .filter(value -> !value.isBlank()).distinct().toList();
    assertEquals(expectedAccounts, usernames.size(), "All owned users must be recorded before cleanup");
    var client = Jdbc.getClient(Databases.AUTH);
    usernames.forEach(username -> assertAll("Cleanup of " + username,
        () -> assertFalse(new AuthAccount(client, username).exist()),
        () -> assertFalse(new Authorities(client, username).exist()),
        () -> assertFalse(new Profile(client, username).exist()),
        () -> assertFalse(new Friendships(client, username).exist()),
        () -> assertFalse(new Invitations(client, username).exist())));
    reporter.publishEntry(Map.of("phase", "storeClosed", "removedAccounts", Integer.toString(usernames.size())));
  }

  static Stream<Named<Class<?>>> successfulScenarios() {
    return successfulTargets().map(target -> Named.of(target.getAnnotation(DisplayName.class).value(), target));
  }

  static Stream<Named<Class<?>>> invalidScenarios() {
    return invalidTargets().map(target -> Named.of(target.getAnnotation(DisplayName.class).value(), target));
  }

  static Stream<Class<?>> successfulTargets() {
    return Stream.of(UserExtensionTargets.DefaultUser.class, UserExtensionTargets.RelatedUser.class,
        UserExtensionTargets.MultipleUsers.class);
  }

  static Stream<Class<?>> invalidTargets() {
    return Stream.of(UserExtensionTargets.WrongType.class, UserExtensionTargets.NegativeFriends.class,
        UserExtensionTargets.NegativeInvitations.class);
  }

  static Stream<Class<?>> allTargets() {
    return Stream.concat(Stream.concat(successfulTargets(), invalidTargets()), Stream.of(
        UserExtensionTargets.FailedTest.class, UserExtensionTargets.PartialSetup.class,
        UserExtensionTargets.UnannotatedUser.class, UserExtensionTargets.NoUser.class));
  }
}

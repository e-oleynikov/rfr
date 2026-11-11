package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.jupiter.fixture.StaticUserFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@DisplayName("UsersQueueExtension: выдача и возврат пользователей пула")
class UsersQueueExtensionTest {

  @AfterEach
  void allThreeUsersAreAvailableAfterTest(TestReporter reporter) {
    try (var first = new StaticUserFixture();
         var second = new StaticUserFixture();
         var third = new StaticUserFixture();
         var fourth = new StaticUserFixture(Duration.ofMillis(25))) {
      first.create();
      second.create();
      third.create();
      assertEquals(3, Set.of(first.user().username(), second.user().username(), third.user().username()).size());
      assertThrows(IllegalStateException.class, fourth::create);
    }
    reporter.publishEntry("returnedPoolUsers", "3");
  }

  @Test
  @DisplayName("Четыре последовательных теста проходят с пулом из трёх пользователей")
  void reusesUsersAcrossFourTests(TestReporter reporter) {
    var results = execute(UsersQueueExtensionTargets.FourSequentialTests.class);
    results.testEvents().assertStatistics(stats -> stats.started(4).succeeded(4).failed(0));
    assertEquals(0, results.containerEvents().failed().count());
    var usernames = usernames(results, reporter);
    assertEquals(4, usernames.size());
    assertEquals(3, usernames.stream().distinct().count());
    assertEquals(usernames.getFirst(), usernames.getLast());
  }

  @Test
  @DisplayName("После упавшего теста пользователь возвращается в очередь")
  void returnsUserAfterFailedTest(TestReporter reporter) {
    var failed = execute(UsersQueueExtensionTargets.FailedTest.class);
    failed.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
    assertEquals(0, failed.containerEvents().failed().count());
    Throwable failure = failed.testEvents().failed().list().getFirst()
        .getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow();
    assertInstanceOf(AssertionError.class, failure);
    assertEquals("Intentional pooled user target failure", failure.getMessage());
    String borrowed = usernames(failed, reporter).getFirst();

    var next = execute(UsersQueueExtensionTargets.BorrowWholePool.class);
    next.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
    assertEquals(0, next.containerEvents().failed().count());
    var returned = usernames(next, reporter);
    assertEquals(3, returned.size());
    assertTrue(returned.contains(borrowed), "The exact user borrowed by the failed test must be available");
  }

  @Test
  @DisplayName("Мишени не запускаются вне EngineTestKit")
  void targetsAreDisabledWithoutEngineTestKitParameter() {
    for (Class<?> target : List.of(UsersQueueExtensionTargets.FourSequentialTests.class,
        UsersQueueExtensionTargets.FailedTest.class, UsersQueueExtensionTargets.BorrowWholePool.class)) {
      var results = EngineTestKit.engine("junit-jupiter").selectors(selectClass(target)).execute();
      assertEquals(1, results.containerEvents().skipped().count());
      assertEquals(0, results.testEvents().started().count());
      assertEquals(0, results.allEvents().failed().count());
    }
  }

  private EngineExecutionResults execute(Class<?> target) {
    return EngineTestKit.engine("junit-jupiter").selectors(selectClass(target))
        .configurationParameter(UsersQueueExtensionTargets.ENABLED, "true")
        .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
        .configurationParameter("junit.jupiter.extensions.store.close.autocloseable.enabled", "true")
        .execute();
  }

  private List<String> usernames(EngineExecutionResults results, TestReporter reporter) {
    return results.allEvents().reportingEntryPublished()
        .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
        .peek(reporter::publishEntry)
        .filter(entry -> entry.containsKey("username"))
        .map(entry -> entry.get("username")).toList();
  }
}

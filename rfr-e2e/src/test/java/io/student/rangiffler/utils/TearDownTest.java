package io.student.rangiffler.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TearDownTest {

  @Test
  void defersCleanupAndUsesSnapshotInRegistrationOrder() {
    List<Integer> resources = new ArrayList<>(List.of(1, 2, 3));
    List<Integer> closed = new ArrayList<>();
    TearDown tearDown = new TearDown()
        .withEach(resources.reversed(), resource -> {
          closed.add(resource);
          resources.remove(resource);
        })
        .with(() -> closed.add(0));
    resources.add(4);
    assertTrue(closed.isEmpty());

    tearDown.run();

    assertEquals(List.of(3, 2, 1, 0), closed);
    assertEquals(List.of(4), resources);
  }

  @Test
  void continuesAfterFailuresAndPreservesSuppressedExceptions() {
    RuntimeException first = new IllegalStateException("First resource");
    RuntimeException second = new IllegalArgumentException("Second resource");
    RuntimeException third = new IllegalStateException("Delegate");
    List<String> completed = new ArrayList<>();
    TearDown tearDown = new TearDown()
        .withEach(List.of(first, second), failure -> {
          throw failure;
        })
        .with(() -> completed.add("Remaining resource"))
        .with(() -> {
          throw third;
        });

    assertSame(first, assertThrows(RuntimeException.class, tearDown::run));
    assertArrayEquals(new Throwable[]{second, third}, first.getSuppressed());
    assertEquals(List.of("Remaining resource"), completed);
  }

  @Test
  void repeatedExceptionInstanceDoesNotInterruptCleanup() {
    RuntimeException failure = new IllegalStateException("Shared failure");
    AtomicBoolean completed = new AtomicBoolean();
    TearDown tearDown = new TearDown()
        .with(() -> {
          throw failure;
        })
        .with(() -> {
          throw failure;
        })
        .with(() -> completed.set(true));

    assertSame(failure, assertThrows(RuntimeException.class, tearDown::run));
    assertTrue(completed.get());
    assertEquals(0, failure.getSuppressed().length);
  }

  @Test
  void newCleanupChainRetriesOnlyResourcesLeftAfterFailure() {
    List<Integer> resources = new ArrayList<>(List.of(1, 2, 3));
    List<Integer> attempted = new ArrayList<>();
    AtomicBoolean fail = new AtomicBoolean(true);
    Runnable close = () -> new TearDown()
        .withEach(resources.reversed(), resource -> {
          attempted.add(resource);
          if (resource == 2 && fail.getAndSet(false)) {
            throw new IllegalStateException("Temporary cleanup failure");
          }
          resources.remove(resource);
        })
        .run();

    assertThrows(IllegalStateException.class, close::run);
    assertEquals(List.of(2), resources);
    close.run();
    close.run();
    assertEquals(List.of(3, 2, 1, 2), attempted);
    assertTrue(resources.isEmpty());
  }

  @Test
  void emptyCleanupDoesNothing() {
    assertDoesNotThrow(() -> new TearDown().withEach(List.of(), item -> {
      fail("Empty collection must not invoke cleanup");
    }).run());
  }
}

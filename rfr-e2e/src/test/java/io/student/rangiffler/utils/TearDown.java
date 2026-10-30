package io.student.rangiffler.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Executes cleanup actions in registration order, collecting runtime failures. */
public final class TearDown implements Runnable {

  private final List<Runnable> actions = new ArrayList<>();

  public TearDown with(Runnable action) {
    actions.add(Objects.requireNonNull(action));
    return this;
  }

  /** Captures the current elements; cleanup may modify the original collection. */
  public <T> TearDown withEach(Iterable<? extends T> items, Consumer<? super T> action) {
    Objects.requireNonNull(items);
    Objects.requireNonNull(action);
    for (T item : items) {
      with(() -> action.accept(item));
    }
    return this;
  }

  /** Each invocation executes all registered actions again. */
  @Override
  public void run() {
    RuntimeException failure = null;
    for (Runnable action : actions) {
      try {
        action.run();
      } catch (RuntimeException e) {
        if (failure == null) {
          failure = e;
        } else if (failure != e) {
          failure.addSuppressed(e);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }
}

package io.student.rangiffler.utils;

import io.student.rangiffler.jupiter.fixture.TestUser;

import java.util.UUID;

public final class RandomDataUtils {

  private RandomDataUtils() {
  }

  public static String randomUsername() {
    return "ui_registration_" + UUID.randomUUID().toString().replace("-", "");
  }

  public static String randomPassword() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  public static TestUser randomUser() {
    return new TestUser(randomUsername(), randomPassword());
  }
}

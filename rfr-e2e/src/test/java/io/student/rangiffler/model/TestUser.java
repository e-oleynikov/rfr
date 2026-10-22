package io.student.rangiffler.model;

import java.util.UUID;

public record TestUser(String username, String password) {

  public static TestUser unique() {
    return new TestUser(
        "ui_registration_" + UUID.randomUUID().toString().replace("-", ""),
        UUID.randomUUID().toString().substring(0, 12)
    );
  }

  @Override
  public String toString() {
    return username;
  }
}

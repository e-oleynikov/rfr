package io.student.rangiffler.data.db;

final class TestUsername {

  private TestUsername() {
  }

  static void requireForDeletion(String username) {
    if (username == null || !username.matches("ui_registration_[0-9a-f]{32}")) {
      throw new IllegalArgumentException("Cleanup is restricted to registration test users");
    }
  }
}

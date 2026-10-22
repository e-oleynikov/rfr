package io.student.rangiffler.jupiter.fixture;

public record TestUser(String username, String password) {

  @Override
  public String toString() {
    return username;
  }
}

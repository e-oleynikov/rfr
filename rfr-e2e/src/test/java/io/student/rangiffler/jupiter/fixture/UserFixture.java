package io.student.rangiffler.jupiter.fixture;

public interface UserFixture extends AutoCloseable {

  void create();

  TestUser user();

  @Override
  void close();
}

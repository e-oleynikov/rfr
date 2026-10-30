package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Objects;

public final class Profile {

  private static final String CREATE = Jdbc.loadSql("sql/api/create-user.sql");
  private static final String DELETE = Jdbc.loadSql("sql/api/delete-user.sql");
  private static final String EXIST = Jdbc.loadSql("sql/api/user-exists.sql");

  private final JdbcClient client;
  private final String username;

  public Profile(JdbcClient client, String username) {
    this.client = Objects.requireNonNull(client);
    this.username = Objects.requireNonNull(username);
  }

  public void create() {
    client.sql(CREATE).param("username", username).update();
  }

  public void delete() {
    TestUsername.requireForDeletion(username);
    client.sql(DELETE).param("username", username).update();
  }

  public boolean exist() {
    return client.sql(EXIST).param("username", username).query(Boolean.class).single();
  }
}

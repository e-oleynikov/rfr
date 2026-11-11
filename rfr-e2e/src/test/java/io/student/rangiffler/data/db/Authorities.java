package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Objects;

public final class Authorities {

  private static final String CREATE = Jdbc.loadSql("sql/auth/grant-authorities.sql");
  private static final String DELETE = Jdbc.loadSql("sql/auth/delete-authorities.sql");
  private static final String EXIST = Jdbc.loadSql("sql/auth/authorities-exist.sql");

  private final JdbcClient client;
  private final String username;

  public Authorities(JdbcClient client, String username) {
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

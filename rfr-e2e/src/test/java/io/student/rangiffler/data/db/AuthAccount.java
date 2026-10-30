package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import java.util.Objects;

public final class AuthAccount {

  private static final String CREATE = Jdbc.loadSql("sql/auth/create-user.sql");
  private static final String DELETE = Jdbc.loadSql("sql/auth/delete-user.sql");
  private static final String EXIST = Jdbc.loadSql("sql/auth/user-exists.sql");

  private final JdbcClient client;
  private final String username;

  public AuthAccount(JdbcClient client, String username) {
    this.client = Objects.requireNonNull(client);
    this.username = Objects.requireNonNull(username);
  }

  public void create(String password) {
    client.sql(CREATE).param("username", username)
        .param("password", PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(password)).update();
  }

  public void delete() {
    TestUsername.requireForDeletion(username);
    client.sql(DELETE).param("username", username).update();
  }

  public boolean exist() {
    return client.sql(EXIST).param("username", username).query(Boolean.class).single();
  }
}

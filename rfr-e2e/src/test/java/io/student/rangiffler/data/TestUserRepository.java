package io.student.rangiffler.data;

import io.qameta.allure.Step;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.jupiter.fixture.TestUser;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class TestUserRepository {

  private static final String CREATE_USER_SQL = Jdbc.loadSql("sql/auth/create-user.sql");
  private static final String GRANT_AUTHORITIES_SQL = Jdbc.loadSql("sql/auth/grant-authorities.sql");
  private static final String DELETE_API_USER_SQL = Jdbc.loadSql("sql/api/delete-user.sql");
  private static final String DELETE_AUTHORITIES_SQL = Jdbc.loadSql("sql/auth/delete-authorities.sql");
  private static final String DELETE_AUTH_USER_SQL = Jdbc.loadSql("sql/auth/delete-user.sql");
  private static final String COUNT_USER_SQL = Jdbc.loadSql("sql/users/count-user.sql");

  private final JdbcClient authClient;
  private final PasswordEncoder passwordEncoder;

  public TestUserRepository() {
    authClient = Jdbc.getClient(Databases.AUTH);
    passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Step("Создать пользователя {user} в базе авторизации")
  public void create(TestUser user) {
    Jdbc.inTransaction(Databases.AUTH, client -> {
      client.sql(CREATE_USER_SQL)
          .param("username", user.username())
          .param("password", passwordEncoder.encode(user.password()))
          .update();
      client.sql(GRANT_AUTHORITIES_SQL)
          .param("username", user.username())
          .update();
    });
  }

  @Step("Удалить тестового пользователя {testUsername} из баз API и авторизации")
  public void delete(String testUsername) {
    if (!testUsername.matches("ui_registration_[0-9a-f]{32}")) {
      throw new IllegalArgumentException("Cleanup is restricted to registration test users");
    }
    Jdbc.inTransaction(Databases.AUTH, client -> {
      client.sql(DELETE_API_USER_SQL).param("username", testUsername).update();
      client.sql(DELETE_AUTHORITIES_SQL).param("username", testUsername).update();
      client.sql(DELETE_AUTH_USER_SQL).param("username", testUsername).update();
      verifyUserAbsent(client, testUsername);
    });
  }

  public void verifyUserAbsent(String testUsername) {
    verifyUserAbsent(authClient, testUsername);
  }

  private void verifyUserAbsent(JdbcClient client, String testUsername) {
    long count = client.sql(COUNT_USER_SQL).param("username", testUsername).query(Long.class).single();
    if (count != 0) {
      throw new IllegalStateException("Test user still exists: " + testUsername);
    }
  }
}

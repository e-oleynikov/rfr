package io.student.rangiffler.data;

import io.qameta.allure.Step;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TestUserCleaner {

  private final String url;
  private final String username;
  private final String password;

  public TestUserCleaner() {
    url = System.getenv().getOrDefault("RFR_DB_URL",
        "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000");
    username = System.getenv().getOrDefault("RFR_DB_USER", "root");
    password = System.getenv("RFR_DB_PASSWORD");
    if (password == null) {
      throw new IllegalStateException("Set RFR_DB_PASSWORD to enable test-user cleanup");
    }
  }

  public void verifyUserAbsent(String testUsername) throws SQLException {
    try (Connection connection = connect()) {
      verifyUserAbsent(connection, testUsername);
    }
  }

  @Step("Удалить тестового пользователя {testUsername} из баз API и авторизации")
  public void delete(String testUsername) throws SQLException {
    if (!testUsername.matches("ui_registration_[0-9a-f]{32}")) {
      throw new IllegalArgumentException("Cleanup is restricted to registration test users");
    }
    try (Connection connection = connect()) {
      connection.setAutoCommit(false);
      try {
        execute(connection, "DELETE FROM `rangiffler-api`.`user` WHERE username = ?", testUsername);
        execute(connection, """
            DELETE FROM `rangiffler-auth`.`authority`
            WHERE user_id IN (SELECT id FROM `rangiffler-auth`.`user` WHERE username = ?)
            """, testUsername);
        execute(connection, "DELETE FROM `rangiffler-auth`.`user` WHERE username = ?", testUsername);
        verifyUserAbsent(connection, testUsername);
        connection.commit();
      } catch (SQLException | RuntimeException e) {
        try {
          connection.rollback();
        } catch (SQLException rollbackFailure) {
          e.addSuppressed(rollbackFailure);
        }
        throw e;
      }
    }
  }

  private Connection connect() throws SQLException {
    return DriverManager.getConnection(url, username, password);
  }

  private void execute(Connection connection, String sql, String testUsername) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, testUsername);
      statement.executeUpdate();
    }
  }

  private void verifyUserAbsent(Connection connection, String testUsername) throws SQLException {
    String sql = """
        SELECT
          (SELECT COUNT(*) FROM `rangiffler-api`.`user` WHERE username = ?) +
          (SELECT COUNT(*) FROM `rangiffler-auth`.`user` WHERE username = ?)
        """;
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, testUsername);
      statement.setString(2, testUsername);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next() || result.getInt(1) != 0) {
          throw new IllegalStateException("Test user still exists: " + testUsername);
        }
      }
    }
  }
}

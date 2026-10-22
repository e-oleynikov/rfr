package io.student.rangiffler.config;

import java.util.Locale;

public final class TestConfig {

  private static final String DEFAULT_FRONT_URL = "http://localhost:3001";
  private static final String DEFAULT_AUTH_URL = "http://localhost:9001";
  private static final String DEFAULT_DB_URL =
      "jdbc:mysql://localhost:3306/?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000";
  private static final String DEFAULT_DB_USER = "root";
  private static final String DEFAULT_HEADED = "false";

  private TestConfig() {
  }

  public static String frontUrl() {
    return setting("front.url", DEFAULT_FRONT_URL);
  }

  public static String authUrl() {
    return setting("auth.url", DEFAULT_AUTH_URL);
  }

  public static String dbUrl() {
    return setting("db.url", DEFAULT_DB_URL);
  }

  public static String dbUser() {
    return setting("db.user", DEFAULT_DB_USER);
  }

  public static String dbPassword() {
    String password = setting("db.password", null);
    if (password == null) {
      throw new IllegalStateException("Set rfr.db.password or RFR_DB_PASSWORD to enable test-user cleanup");
    }
    return password;
  }

  public static boolean headed() {
    return Boolean.parseBoolean(setting("headed", DEFAULT_HEADED));
  }

  private static String setting(String key, String fallback) {
    String property = System.getProperty("rfr." + key);
    if (property != null) {
      return property;
    }
    String environmentKey = "RFR_" + key.toUpperCase(Locale.ROOT).replace('.', '_');
    return System.getenv().getOrDefault(environmentKey, fallback);
  }
}

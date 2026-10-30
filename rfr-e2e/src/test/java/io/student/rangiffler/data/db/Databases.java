package io.student.rangiffler.data.db;

import io.student.rangiffler.config.TestConfig;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

public enum Databases {
  AUTH("rangiffler-auth"),
  API("rangiffler-api");

  private final DataSource dataSource;
  private final PlatformTransactionManager transactionManager;

  Databases(String catalog) {
    DriverManagerDataSource source = new DriverManagerDataSource(
        TestConfig.dbUrl(), TestConfig.dbUser(), TestConfig.dbPassword());
    source.setCatalog(catalog);
    dataSource = source;
    transactionManager = new JdbcTransactionManager(dataSource);
  }

  public DataSource dataSource() {
    return dataSource;
  }

  public PlatformTransactionManager transactionManager() {
    return transactionManager;
  }
}

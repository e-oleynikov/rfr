package io.student.rangiffler.data.db;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class Jdbc {

  private Jdbc() {
  }

  public static JdbcClient getClient(Databases database) {
    return JdbcClient.create(database.dataSource());
  }

  public static void inTransaction(Databases database, Consumer<JdbcClient> action) {
    new TransactionTemplate(database.transactionManager())
        .executeWithoutResult(status -> action.accept(getClient(database)));
  }

  public static String loadSql(String resourcePath) {
    try {
      return new ClassPathResource(resourcePath).getContentAsString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot load SQL resource: " + resourcePath, e);
    }
  }
}

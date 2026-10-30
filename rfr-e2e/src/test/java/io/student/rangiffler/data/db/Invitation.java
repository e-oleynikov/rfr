package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.Objects;

public final class Invitation {

  private static final String CREATE = Jdbc.loadSql("sql/api/create-invitation.sql");
  private static final String DELETE = Jdbc.loadSql("sql/api/delete-invitation.sql");
  private static final String EXIST = Jdbc.loadSql("sql/api/invitation-exists.sql");

  private final JdbcClient client;
  private final String requesterUsername;
  private final String addresseeUsername;

  public Invitation(JdbcClient client, String requesterUsername, String addresseeUsername) {
    this.client = Objects.requireNonNull(client);
    this.requesterUsername = Objects.requireNonNull(requesterUsername);
    this.addresseeUsername = Objects.requireNonNull(addresseeUsername);
  }

  public void create() {
    query(CREATE).update();
  }

  public void delete() {
    TestUsername.requireForDeletion(requesterUsername);
    TestUsername.requireForDeletion(addresseeUsername);
    query(DELETE).update();
  }

  public boolean exist() {
    return query(EXIST).query(Boolean.class).single();
  }

  private JdbcClient.StatementSpec query(String sql) {
    return client.sql(sql)
        .param("requesterUsername", requesterUsername)
        .param("addresseeUsername", addresseeUsername);
  }
}

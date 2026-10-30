package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Objects;

public final class Friendships {

  private static final String DELETE = Jdbc.loadSql("sql/api/delete-friends.sql");
  private static final String EXIST = Jdbc.loadSql("sql/api/user-relationships-exist.sql");
  private static final String SELECT = Jdbc.loadSql("sql/api/select-user-relationships.sql");

  private final JdbcClient client;
  private final String username;

  public Friendships(JdbcClient client, String username) {
    this.client = Objects.requireNonNull(client);
    this.username = Objects.requireNonNull(username);
  }

  public void delete() {
    TestUsername.requireForDeletion(username);
    client.sql(DELETE).param("username", username).update();
  }

  public boolean exist() {
    return query(EXIST).query(Boolean.class).single();
  }

  public List<Relationship> list() {
    return List.copyOf(query(SELECT).query((rs, row) -> new Relationship(
        rs.getString("requester"), rs.getString("addressee"),
        rs.getString("status"), rs.getTimestamp("responded_at") != null)).list());
  }

  private JdbcClient.StatementSpec query(String sql) {
    return client.sql(sql).param("username", username).param("status", "ACCEPTED");
  }
}

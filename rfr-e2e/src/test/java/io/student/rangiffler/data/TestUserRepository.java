package io.student.rangiffler.data;

import io.qameta.allure.Step;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Authorities;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.FriendShip;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Invitation;
import io.student.rangiffler.data.db.Invitations;
import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.jupiter.fixture.TestUser;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class TestUserRepository {

  private final JdbcClient authClient;

  public TestUserRepository() {
    authClient = Jdbc.getClient(Databases.AUTH);
  }

  @Step("Создать пользователя {user} в базе авторизации")
  public void create(TestUser user) {
    Jdbc.inTransaction(Databases.AUTH, client -> {
      new AuthAccount(client, user.username()).create(user.password());
      new Authorities(client, user.username()).create();
    });
  }

  public void createApiProfile(TestUser user) {
    new Profile(authClient, user.username()).create();
  }

  public void createFriendship(TestUser requester, TestUser addressee) {
    new FriendShip(authClient, requester.username(), addressee.username()).create();
  }

  public void createInvitation(TestUser requester, TestUser addressee) {
    new Invitation(authClient, requester.username(), addressee.username()).create();
  }

  @Step("Удалить тестового пользователя {testUsername} из баз API и авторизации")
  public void delete(String testUsername) {
    Jdbc.inTransaction(Databases.AUTH, client -> {
      new Friendships(client, testUsername).delete();
      new Invitations(client, testUsername).delete();
      new Profile(client, testUsername).delete();
      new Authorities(client, testUsername).delete();
      new AuthAccount(client, testUsername).delete();
      verifyUserAbsent(client, testUsername);
    });
  }

  public void verifyUserAbsent(String testUsername) {
    verifyUserAbsent(authClient, testUsername);
  }

  private void verifyUserAbsent(JdbcClient client, String testUsername) {
    if (new AuthAccount(client, testUsername).exist() || new Profile(client, testUsername).exist()) {
      throw new IllegalStateException("Test user still exists: " + testUsername);
    }
  }
}

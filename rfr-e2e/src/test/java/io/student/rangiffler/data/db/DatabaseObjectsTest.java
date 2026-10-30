package io.student.rangiffler.data.db;

import io.student.rangiffler.data.TestUserRepository;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.jupiter.fixture.UserFixture;
import io.student.rangiffler.jupiter.fixture.UserRequest;
import io.student.rangiffler.utils.RandomDataUtils;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseObjectsTest {

  private final JdbcClient client = Jdbc.getClient(Databases.AUTH);

  @Test
  void accountAuthoritiesAndProfileReflectDatabaseChanges() {
    TestUser user = RandomDataUtils.randomUser();
    AuthAccount account = new AuthAccount(client, user.username());
    Authorities authorities = new Authorities(client, user.username());
    Profile profile = new Profile(client, user.username());
    try {
      assertFalse(account.exist());
      assertFalse(authorities.exist());
      assertFalse(profile.exist());
      account.create(user.password());
      authorities.create();
      profile.create();
      profile.create();
      assertTrue(account.exist());
      assertTrue(authorities.exist());
      assertTrue(profile.exist());
      profile.delete();
      authorities.delete();
      account.delete();
      assertFalse(profile.exist());
      assertFalse(authorities.exist());
      assertFalse(account.exist());
    } finally {
      new TestUserRepository().delete(user.username());
    }
  }

  @Test
  void friendshipIsSymmetricButInvitationIsDirectional() {
    try (UserFixture first = new UserRequest(0, 0).fixture();
         UserFixture second = new UserRequest(0, 0).fixture()) {
      createProfiled(first);
      createProfiled(second);
      String a = first.user().username();
      String b = second.user().username();
      FriendShip friendship = new FriendShip(client, a, b);
      FriendShip reverseFriendship = new FriendShip(client, b, a);
      Invitation invitation = new Invitation(client, a, b);
      Invitation reverseInvitation = new Invitation(client, b, a);
      assertFalse(friendship.exist());
      invitation.create();
      assertTrue(invitation.exist());
      assertFalse(reverseInvitation.exist());
      assertFalse(friendship.exist());
      reverseInvitation.delete();
      friendship.delete();
      assertTrue(invitation.exist());
      invitation.delete();
      assertFalse(invitation.exist());
      friendship.create();
      assertTrue(friendship.exist());
      assertTrue(reverseFriendship.exist());
      invitation.delete();
      assertTrue(friendship.exist());
      reverseFriendship.delete();
      assertFalse(friendship.exist());
    }
  }

  @Test
  void collectionDeletionSeparatesStatusesAndPreservesOtherUsers() {
    try (UserFixture owner = new UserRequest(1, 1).fixture();
         UserFixture other = new UserRequest(1, 1).fixture()) {
      owner.create();
      other.create();
      String a = owner.user().username();
      String b = other.user().username();
      new Invitation(client, a, b).create();
      Friendships friends = new Friendships(client, a);
      Invitations invitations = new Invitations(client, a);
      assertTrue(friends.exist());
      assertTrue(invitations.exist());
      var snapshot = invitations.list();
      assertEquals(2, snapshot.size());
      assertThrows(UnsupportedOperationException.class, snapshot::clear);
      friends.delete();
      assertFalse(friends.exist());
      assertTrue(invitations.exist());
      invitations.delete();
      assertFalse(invitations.exist());
      assertEquals(2, snapshot.size());
      assertEquals(1, new Friendships(client, b).list().size());
      assertEquals(1, new Invitations(client, b).list().size());
      assertTrue(new AuthAccount(client, b).exist());
      assertTrue(new Profile(client, b).exist());
    }
  }

  @Test
  void deletionRollsBackBothSchemasOnTheSameTransaction() {
    try (UserFixture fixture = new UserRequest(1, 1).fixture()) {
      fixture.create();
      String username = fixture.user().username();
      RuntimeException failure = new IllegalStateException("Force transaction rollback");
      assertSame(failure, assertThrows(IllegalStateException.class, () ->
          Jdbc.inTransaction(Databases.AUTH, transactionClient -> {
            new Friendships(transactionClient, username).delete();
            new Invitations(transactionClient, username).delete();
            new Profile(transactionClient, username).delete();
            new Authorities(transactionClient, username).delete();
            new AuthAccount(transactionClient, username).delete();
            assertFalse(new AuthAccount(transactionClient, username).exist());
            assertFalse(new Profile(transactionClient, username).exist());
            throw failure;
          })));
      assertAll(
          () -> assertTrue(new AuthAccount(client, username).exist()),
          () -> assertTrue(new Authorities(client, username).exist()),
          () -> assertTrue(new Profile(client, username).exist()),
          () -> assertTrue(new Friendships(client, username).exist()),
          () -> assertTrue(new Invitations(client, username).exist()));
    }
  }

  @Test
  void directDeletionRejectsNonTestUsernames() {
    String ordinary = "ordinary_user";
    String test = RandomDataUtils.randomUser().username();
    assertAll(
        () -> assertThrows(IllegalArgumentException.class, new AuthAccount(client, ordinary)::delete),
        () -> assertThrows(IllegalArgumentException.class, new Authorities(client, ordinary)::delete),
        () -> assertThrows(IllegalArgumentException.class, new Profile(client, ordinary)::delete),
        () -> assertThrows(IllegalArgumentException.class, new Friendships(client, ordinary)::delete),
        () -> assertThrows(IllegalArgumentException.class, new Invitations(client, ordinary)::delete),
        () -> assertThrows(IllegalArgumentException.class, new FriendShip(client, ordinary, test)::delete),
        () -> assertThrows(IllegalArgumentException.class, new FriendShip(client, test, ordinary)::delete),
        () -> assertThrows(IllegalArgumentException.class, new Invitation(client, ordinary, test)::delete),
        () -> assertThrows(IllegalArgumentException.class, new Invitation(client, test, ordinary)::delete));
  }

  private void createProfiled(UserFixture fixture) {
    fixture.create();
    new Profile(client, fixture.user().username()).create();
  }
}

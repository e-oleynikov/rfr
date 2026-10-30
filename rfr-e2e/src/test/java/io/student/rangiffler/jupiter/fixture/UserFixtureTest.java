package io.student.rangiffler.jupiter.fixture;

import io.student.rangiffler.data.TestUserRepository;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Invitations;
import io.student.rangiffler.data.db.Jdbc;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.data.db.Relationship;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.UserExtension;
import io.student.rangiffler.utils.RandomDataUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserFixtureTest {

  private final TestUserRepository repository = new TestUserRepository();

  @ParameterizedTest
  @CsvSource({"0, 0", "2, 0", "0, 3", "2, 3"})
  void createsRequestedRelationshipsAndCleansAllUsers(int friends, int invitations) {
    UserFixture fixture = new UserRequest(friends, invitations).fixture();
    String username = fixture.user().username();
    List<String> usernames = new ArrayList<>(List.of(username));
    repository.verifyUserAbsent(username);
    try (fixture) {
      fixture.create();
      List<Relationship> relationships = relationships(username);
      assertEquals(relationships.stream().filter(link -> link.status().equals("ACCEPTED"))
              .map(link -> link.otherUser(username)).sorted().toList(),
          fixture.user().friends().stream().map(TestUser::username).sorted().toList());
      assertEquals(relationships.stream().filter(link -> link.status().equals("PENDING"))
              .map(Relationship::requester).sorted().toList(),
          fixture.user().incomingInvitationSenders().stream().map(TestUser::username).sorted().toList());
      relationships.forEach(link -> usernames.add(link.otherUser(username)));
      assertEquals(friends, relationships.stream().filter(link -> link.status().equals("ACCEPTED")).count());
      assertEquals(invitations, relationships.stream().filter(link -> link.status().equals("PENDING")).count());
      for (Relationship link : relationships) {
        assertEquals(link.status().equals("ACCEPTED"), link.responded());
        if (link.status().equals("PENDING")) {
          assertEquals(username, link.addressee(), "Invitation must be incoming");
        }
      }
      assertThrows(IllegalStateException.class, fixture::create);
    }
    fixture.close();
    assertEquals(friends, fixture.user().friends().size());
    assertEquals(invitations, fixture.user().incomingInvitationSenders().size());
    usernames.forEach(repository::verifyUserAbsent);
  }

  @Test
  void cleanupPreservesAnotherFixture() {
    try (UserFixture other = new UserRequest(1, 1).fixture()) {
      other.create();
      List<Relationship> before = relationships(other.user().username());
      try (UserFixture fixture = new UserRequest(2, 2).fixture()) {
        fixture.create();
      }
      assertEquals(before, relationships(other.user().username()));
    }
  }

  @ParameterizedTest
  @CsvSource({"ACCEPTED, true", "ACCEPTED, false", "PENDING, true", "PENDING, false"})
  void singleUserCleanupRemovesRelationshipsCreatedDuringTest(String status, boolean outgoing) {
    try (UserFixture other = new SingeUserFixture(repository, RandomDataUtils.randomUser())) {
      other.create();
      repository.createApiProfile(other.user());
      UserFixture fixture = new SingeUserFixture(repository, RandomDataUtils.randomUser());
      try (fixture) {
        fixture.create();
        repository.createApiProfile(fixture.user());
        TestUser requester = outgoing ? fixture.user() : other.user();
        TestUser addressee = outgoing ? other.user() : fixture.user();
        if (status.equals("ACCEPTED")) {
          repository.createFriendship(requester, addressee);
        } else {
          repository.createInvitation(requester, addressee);
        }
        assertEquals(1, relationships(fixture.user().username()).size());
      }
      fixture.close();
      repository.verifyUserAbsent(fixture.user().username());
      assertTrue(relationships(other.user().username()).isEmpty());
      var client = Jdbc.getClient(Databases.AUTH);
      assertTrue(new AuthAccount(client, other.user().username()).exist());
      assertTrue(new Profile(client, other.user().username()).exist());
    }
  }

  @Test
  void cleanupCoversFailureAfterOneRelationshipWasCreated() {
    List<String> usernames = new ArrayList<>();
    UserFixture base = new SingeUserFixture(repository, RandomDataUtils.randomUser());
    usernames.add(base.user().username());
    UserFixture fixture = new UserRelationshipFixture(base, 3) {
      @Override
      protected TestUser createRelationship(TestUser relatedUser) {
        usernames.add(relatedUser.username());
        if (usernames.size() == 3) {
          throw new IllegalStateException("Simulated relationship failure");
        }
        repository.createFriendship(user(), relatedUser);
        return user().withFriend(relatedUser);
      }
    };
    try (fixture) {
      IllegalStateException error = assertThrows(IllegalStateException.class, fixture::create);
      assertEquals("Simulated relationship failure", error.getMessage());
      assertEquals(1, relationships(base.user().username()).size());
      assertEquals(1, fixture.user().friends().size());
    }
    assertEquals(3, usernames.size());
    usernames.forEach(repository::verifyUserAbsent);
  }

  @Test
  @ExtendWith(UserExtension.class)
  void extensionUsesAnnotationCounts(@User(friends = 1, incomingInvitations = 2) TestUser user) {
    assertEquals(1, user.friends().size());
    assertEquals(2, user.incomingInvitationSenders().size());
    List<Relationship> relationships = relationships(user.username());
    assertEquals(1, relationships.stream().filter(link -> link.status().equals("ACCEPTED")).count());
    assertEquals(2, relationships.stream()
        .filter(link -> link.status().equals("PENDING") && link.addressee().equals(user.username())).count());
  }

  @Test
  void decoratorsPreserveListsInReverseOrderAndWhenRepeated() {
    UserFixture base = new SingeUserFixture(repository, RandomDataUtils.randomUser());
    try (UserFixture fixture = new UserWithFriendFixture(
        new UserWithFriendFixture(new UserWithIncomeInvitations(base, 2), 1), 1)) {
      fixture.create();
      assertEquals(base.user().username(), fixture.user().username());
      assertEquals(base.user().password(), fixture.user().password());
      assertEquals(2, fixture.user().friends().size());
      assertEquals(2, fixture.user().incomingInvitationSenders().size());
      assertEquals(4, relationships(fixture.user().username()).size());
      assertTrue(base.user().friends().isEmpty());
      assertTrue(base.user().incomingInvitationSenders().isEmpty());
    }
  }

  private List<Relationship> relationships(String username) {
    var client = Jdbc.getClient(Databases.API);
    List<Relationship> relationships = new ArrayList<>(new Friendships(client, username).list());
    relationships.addAll(new Invitations(client, username).list());
    return List.copyOf(relationships);
  }
}

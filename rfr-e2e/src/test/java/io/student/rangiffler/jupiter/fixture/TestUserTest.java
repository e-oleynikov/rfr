package io.student.rangiffler.jupiter.fixture;

import io.student.rangiffler.utils.RandomDataUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestUserTest {

  @Test
  void copiesListsAndExposesImmutableSnapshots() {
    TestUser friend = RandomDataUtils.randomUser();
    TestUser sender = RandomDataUtils.randomUser();
    List<TestUser> friends = new ArrayList<>(List.of(friend));
    List<TestUser> senders = new ArrayList<>(List.of(sender));
    TestUser credentials = RandomDataUtils.randomUser();
    TestUser user = new TestUser(credentials.username(), credentials.password(), friends, senders);
    friends.clear();
    senders.clear();

    assertEquals(List.of(friend), user.friends());
    assertEquals(List.of(sender), user.incomingInvitationSenders());
    assertThrows(UnsupportedOperationException.class, () -> user.friends().add(sender));
    assertThrows(UnsupportedOperationException.class, () -> user.incomingInvitationSenders().clear());
    assertEquals(credentials.username(), user.toString());
  }

  @Test
  void enrichmentPreservesCredentialsAndPreviousRelationshipsWithoutMutatingOriginal() {
    TestUser original = RandomDataUtils.randomUser();
    TestUser firstFriend = RandomDataUtils.randomUser();
    TestUser secondFriend = RandomDataUtils.randomUser();
    TestUser sender = RandomDataUtils.randomUser();
    TestUser enriched = original.withFriend(firstFriend)
        .withIncomingInvitationSender(sender).withFriend(secondFriend);

    assertEquals(original.username(), enriched.username());
    assertEquals(original.password(), enriched.password());
    assertEquals(List.of(firstFriend, secondFriend), enriched.friends());
    assertEquals(List.of(sender), enriched.incomingInvitationSenders());
    assertTrue(original.friends().isEmpty());
    assertTrue(original.incomingInvitationSenders().isEmpty());
  }
}

package io.student.rangiffler.jupiter.fixture;

import java.util.List;
import java.util.stream.Stream;

public record TestUser(
    String username,
    String password,
    List<TestUser> friends,
    List<TestUser> incomingInvitationSenders,
    List<TestUser> outcomeInvitations
) {

  public TestUser {
    friends = List.copyOf(friends);
    incomingInvitationSenders = List.copyOf(incomingInvitationSenders);
    outcomeInvitations = List.copyOf(outcomeInvitations);
  }

  public TestUser(String username, String password) {
    this(username, password, List.of(), List.of(), List.of());
  }

  public TestUser withFriend(TestUser friend) {
    return new TestUser(username, password,
        Stream.concat(friends.stream(), Stream.of(friend)).toList(), incomingInvitationSenders,
        outcomeInvitations);
  }

  public TestUser withIncomingInvitationSender(TestUser sender) {
    return new TestUser(username, password, friends,
        Stream.concat(incomingInvitationSenders.stream(), Stream.of(sender)).toList(), outcomeInvitations);
  }

  public TestUser withOutcomeInvitations(TestUser invitee) {
    return new TestUser(username, password, friends, incomingInvitationSenders,
        Stream.concat(outcomeInvitations.stream(), Stream.of(invitee)).toList());
  }

  @Override
  public String toString() {
    return username;
  }
}

package io.student.rangiffler.jupiter.fixture;

public final class UserWithIncomeInvitations extends UserRelationshipFixture {

  public UserWithIncomeInvitations(UserFixture delegate, int incomingInvitations) {
    super(delegate, incomingInvitations);
  }

  @Override
  protected TestUser createRelationship(TestUser relatedUser) {
    repository.createInvitation(relatedUser, user());
    return user().withIncomingInvitationSender(relatedUser);
  }
}

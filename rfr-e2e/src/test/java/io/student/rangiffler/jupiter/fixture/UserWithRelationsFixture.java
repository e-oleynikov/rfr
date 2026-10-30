package io.student.rangiffler.jupiter.fixture;

import java.util.Objects;

public final class UserWithRelationsFixture extends UserRelationshipFixture {

  private final Relation relation;

  public UserWithRelationsFixture(UserFixture origin, Relation relation, int count) {
    super(origin, count);
    this.relation = Objects.requireNonNull(relation);
  }

  @Override
  protected TestUser createRelationship(TestUser relatedUser) {
    return relation.create(repository, user(), relatedUser);
  }
}

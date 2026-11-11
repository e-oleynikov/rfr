package io.student.rangiffler.data.db;

public record Relationship(String requester, String addressee, String status, boolean responded) {

  public String otherUser(String username) {
    return requester.equals(username) ? addressee : requester;
  }
}

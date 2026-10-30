package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.qameta.allure.Feature;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.BrowserExtension;
import io.student.rangiffler.jupiter.extension.UserExtension;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.page.PeoplePage;
import io.student.rangiffler.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Feature("People: Friends")
@ExtendWith({UserExtension.class, BrowserExtension.class})
public class FriendsTabTest {

  @Test
  @DisplayName("Friends: состав списка и элементы управления строкой")
  void shouldShowRelatedUserAndControls(Page page, @User(friends = 1) TestUser user) {
    String relatedUsername = user.friends().getFirst().username();
    login(page, user).open()
        .shouldHaveFriends(1)
        .shouldHaveOnlyUsers(relatedUsername)
        .shouldHaveFriend(relatedUsername);
  }

  @Test
  @DisplayName("Friends: связи других типов не отображаются")
  void shouldExcludeOtherRelationshipTypes(
      Page page, @User(friends = 1, incomingInvitations = 1, outcomeInvitations = 1) TestUser user) {
    login(page, user).open()
        .shouldHaveFriends(1)
        .shouldHaveOnlyUsers(user.friends().getFirst().username())
        .shouldNotHaveUser(user.incomingInvitationSenders().getFirst().username())
        .shouldNotHaveUser(user.outcomeInvitations().getFirst().username());
  }

  @Test
  @DisplayName("Friends: связи другого владельца не отображаются")
  void shouldExcludeAnotherUsersRelationships(
      Page page, @User(friends = 1) TestUser user, @User(friends = 1) TestUser other) {
    login(page, user).open()
        .shouldHaveFriends(1)
        .shouldHaveOnlyUsers(user.friends().getFirst().username())
        .shouldNotHaveUser(other.friends().getFirst().username())
        .shouldNotHaveUser(other.username());
  }

  @Test
  @DisplayName("Friends: пустое состояние для пользователя без связей")
  void shouldShowEmptyState(Page page, @User TestUser user) {
    login(page, user).open()
        .shouldHaveFriends(0)
        .shouldHaveNoUsers();
  }

  private PeoplePage login(Page page, TestUser user) {
    new WelcomePage(page).open().openLogin().fillLoginForm(user).signIn()
        .openProfile().shouldHaveUsername(user.username());
    return new PeoplePage(page);
  }
}

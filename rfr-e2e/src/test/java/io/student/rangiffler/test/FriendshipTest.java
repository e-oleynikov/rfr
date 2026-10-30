package io.student.rangiffler.test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.qameta.allure.Feature;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.BrowserExtension;
import io.student.rangiffler.jupiter.extension.UserExtension;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.page.WelcomePage;
import io.student.rangiffler.ui.page.PeoplePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Feature("Дружба")
@ExtendWith({UserExtension.class, BrowserExtension.class})
public class FriendshipTest {

  @Test
  @DisplayName("Друзья и входящие заявки из @User отображаются в UI")
  void shouldShowPreparedFriendsAndInvitations(Page page, @User(friends = 2, incomingInvitations = 3) TestUser user) {
    login(page, user).open().shouldHaveFriends(2)
        .openIncomingInvitations().shouldHaveIncomingInvitations(3);
  }

  @Test
  @DisplayName("Пользователь без связей отправляет приглашение через UI")
  void shouldSendInvitationFromEmptyUser(Page page, @User TestUser sender, @User TestUser recipient) {
    try (BrowserContext recipientContext = page.context().browser().newContext()) {
      login(recipientContext.newPage(), recipient).open().shouldHaveNoUsers();
      login(page, sender).open().shouldHaveNoUsers()
          .openAllPeople().searchUser(recipient.username()).sendInvitation(recipient.username())
          .openOutgoingInvitations().shouldHaveOutgoingInvitation(recipient.username());
    }
  }

  @Test
  @DisplayName("Два пользователя без связей становятся друзьями через UI")
  void shouldBecomeFriendsThroughUi(Page page, @User TestUser sender, @User TestUser recipient) {
    try (BrowserContext recipientContext = page.context().browser().newContext()) {
      PeoplePage recipientPeople = login(recipientContext.newPage(), recipient).open().shouldHaveNoUsers();
      PeoplePage senderPeople = login(page, sender).open().shouldHaveNoUsers();
      senderPeople.openAllPeople().searchUser(recipient.username()).sendInvitation(recipient.username())
          .openOutgoingInvitations().shouldHaveOutgoingInvitation(recipient.username());
      recipientPeople.openIncomingInvitations().shouldHaveIncomingInvitations(1)
          .acceptInvitation(sender.username()).openFriends().shouldHaveFriend(sender.username());
      senderPeople.open().shouldHaveFriend(recipient.username());
    }
  }

  private PeoplePage login(Page page, TestUser user) {
    new WelcomePage(page).open().openLogin().fillLoginForm(user).signIn()
        .openProfile().shouldHaveUsername(user.username());
    return new PeoplePage(page);
  }
}

package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeoplePage {

  public static String URL = TestConfig.frontUrl() + "/people";

  private final Page page;
  private final Locator friendsTab;
  private final Locator incomingTab;
  private final Locator outgoingTab;
  private final Locator allPeopleTab;
  private final Locator search;
  private final Locator searchButton;
  private final Locator rows;
  private final Locator emptyMessage;

  public PeoplePage(Page page) {
    this.page = page;
    friendsTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Friends").setExact(true));
    incomingTab = page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("Income invitations").setExact(true));
    outgoingTab = page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("Outcome invitations").setExact(true));
    allPeopleTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("All People").setExact(true));
    search = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("search people").setExact(true));
    searchButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("search").setExact(true));
    rows = page.locator("tbody").getByRole(AriaRole.ROW);
    emptyMessage = page.getByText("There are no users yet", new Page.GetByTextOptions().setExact(true));
  }

  @Step("Открыть список друзей")
  public PeoplePage open() {
    awaitOperation("GetFriends", () -> page.navigate(URL));
    return this;
  }

  @Step("Открыть входящие приглашения")
  public PeoplePage openIncomingInvitations() {
    awaitOperation("GetInvitations", incomingTab::click);
    return this;
  }

  @Step("Открыть исходящие приглашения")
  public PeoplePage openOutgoingInvitations() {
    awaitOperation("GetOutcomeInvitations", outgoingTab::click);
    return this;
  }

  @Step("Открыть всех пользователей")
  public PeoplePage openAllPeople() {
    awaitOperation("GetPeople", allPeopleTab::click);
    return this;
  }

  @Step("Открыть вкладку друзей")
  public PeoplePage openFriends() {
    awaitOperation("GetFriends", friendsTab::click);
    return this;
  }

  @Step("Найти пользователя {username}")
  public PeoplePage searchUser(String username) {
    search.fill(username);
    searchButton.click();
    return this;
  }

  @Step("Отправить приглашение пользователю {username}")
  public PeoplePage sendInvitation(String username) {
    awaitOperation("FriendshipAction", () -> userRow(username)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add").setExact(true)).click());
    return this;
  }

  @Step("Принять приглашение пользователя {username}")
  public PeoplePage acceptInvitation(String username) {
    awaitOperation("FriendshipAction", () -> userRow(username)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Accept").setExact(true)).click());
    return this;
  }

  @Step("Проверить количество друзей: {count}")
  public PeoplePage shouldHaveFriends(int count) {
    assertThat(friendsTab).hasAttribute("aria-selected", "true");
    assertThat(rows).hasCount(count);
    assertThat(rows.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove").setExact(true)))
        .hasCount(count);
    return this;
  }

  @Step("Проверить количество входящих приглашений: {count}")
  public PeoplePage shouldHaveIncomingInvitations(int count) {
    assertThat(incomingTab).hasAttribute("aria-selected", "true");
    assertThat(rows).hasCount(count);
    assertThat(rows.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Accept").setExact(true)))
        .hasCount(count);
    return this;
  }

  @Step("Проверить пустой список пользователей")
  public PeoplePage shouldHaveNoUsers() {
    assertThat(emptyMessage).isVisible();
    assertThat(rows).hasCount(0);
    return this;
  }

  @Step("Проверить точный состав списка пользователей: {usernames}")
  public PeoplePage shouldHaveOnlyUsers(String... usernames) {
    assertThat(rows).hasCount(usernames.length);
    for (String username : usernames) {
      assertThat(userRow(username)).isVisible();
    }
    return this;
  }

  @Step("Проверить отсутствие пользователя {username} в списке")
  public PeoplePage shouldNotHaveUser(String username) {
    assertThat(userRow(username)).hasCount(0);
    return this;
  }

  @Step("Проверить количество исходящих приглашений: {count}")
  public PeoplePage shouldHaveOutgoingInvitations(int count) {
    assertThat(outgoingTab).hasAttribute("aria-selected", "true");
    assertThat(rows).hasCount(count);
    return this;
  }

  @Step("Проверить входящее приглашение от {username} и кнопки Accept и Decline")
  public PeoplePage shouldHaveIncomingInvitation(String username) {
    assertThat(incomingTab).hasAttribute("aria-selected", "true");
    assertThat(userRow(username)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Accept").setExact(true))).isVisible();
    assertThat(userRow(username)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Decline").setExact(true))).isVisible();
    return this;
  }

  @Step("Проверить исходящее приглашение пользователю {username}")
  public PeoplePage shouldHaveOutgoingInvitation(String username) {
    assertThat(outgoingTab).hasAttribute("aria-selected", "true");
    assertThat(userRow(username).getByText("Waiting...", new Locator.GetByTextOptions().setExact(true))).isVisible();
    assertThat(userRow(username)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel").setExact(true))).isVisible();
    return this;
  }

  @Step("Проверить друга {username}")
  public PeoplePage shouldHaveFriend(String username) {
    assertThat(friendsTab).hasAttribute("aria-selected", "true");
    assertThat(userRow(username)
        .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Remove").setExact(true))).isVisible();
    return this;
  }

  private Locator userRow(String username) {
    return rows.filter(new Locator.FilterOptions().setHas(page.getByRole(AriaRole.CELL,
        new Page.GetByRoleOptions().setName(username).setExact(true))));
  }

  private void awaitOperation(String operation, Runnable action) {
    Pattern name = Pattern.compile("\"operationName\"\\s*:\\s*\"" + Pattern.quote(operation) + "\"");
    page.waitForResponse(response -> {
      String body = response.request().postData();
      return response.url().endsWith("/graphql") && "POST".equals(response.request().method())
          && body != null && name.matcher(body).find();
    }, action).finished();
  }
}

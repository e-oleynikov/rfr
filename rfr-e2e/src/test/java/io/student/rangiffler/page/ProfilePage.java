package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class ProfilePage {

  public static String URL = TestConfig.frontUrl() + "/profile";

  private final Page page;
  private final Locator heading;
  private final Locator usernameInput;

  public ProfilePage(Page page) {
    this.page = page;
    heading = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("My profile").setExact(true));
    usernameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true));
  }

  @Step("Проверить, что профиль принадлежит пользователю {username}")
  public ProfilePage shouldHaveUsername(String username) {
    assertThat(page).hasURL(URL);
    assertThat(heading).isVisible();
    assertThat(usernameInput).hasValue(username);
    return this;
  }
}

package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class TravelsPage {

  private final Page page;

  public TravelsPage(Page page) {
    this.page = page;
  }

  @Step("Проверить открытие карты путешествий после входа")
  public TravelsPage shouldBeOpened() {
    assertThat(page).hasURL(Pattern.compile(".*/my-travels$"));
    assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Travels map")))
        .isVisible();
    return this;
  }

  @Step("Открыть профиль")
  public ProfilePage openProfile() {
    page.getByRole(AriaRole.BANNER)
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile").setExact(true)).click();
    return new ProfilePage(page);
  }
}

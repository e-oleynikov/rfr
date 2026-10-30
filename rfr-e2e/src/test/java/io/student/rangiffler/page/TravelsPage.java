package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

public final class TravelsPage {

  public static String URL = TestConfig.frontUrl() + "/my-travels";

  private final Page page;
  private final Locator profileLink;

  public TravelsPage(Page page) {
    this.page = page;
    profileLink = page.getByRole(AriaRole.BANNER)
        .getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile").setExact(true));
  }

  @Step("Открыть профиль")
  public ProfilePage openProfile() {
    profileLink.click();
    return new ProfilePage(page);
  }
}

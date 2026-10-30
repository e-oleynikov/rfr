package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

public final class WelcomePage {

  public static String URL = TestConfig.frontUrl() + "/";

  private final Page page;
  private final Locator loginButton;
  private final Locator registrationLink;

  public WelcomePage(Page page) {
    this.page = page;
    loginButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login").setExact(true));
    registrationLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register").setExact(true));
  }

  @Step("Открыть приложение")
  public WelcomePage open() {
    page.navigate(URL);
    return this;
  }

  @Step("Открыть страницу регистрации")
  public RegistrationPage openRegistration() {
    registrationLink.click();
    return new RegistrationPage(page);
  }

  @Step("Открыть страницу входа")
  public LoginPage openLogin() {
    loginButton.click();
    return new LoginPage(page);
  }
}

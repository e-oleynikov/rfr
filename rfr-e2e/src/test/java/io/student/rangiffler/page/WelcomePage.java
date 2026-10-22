package io.student.rangiffler.page;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class WelcomePage {

  private final Page page;

  public WelcomePage(Page page) {
    this.page = page;
  }

  @Step("Открыть приложение")
  public WelcomePage open() {
    page.navigate("/");
    return shouldBeOpened();
  }

  @Step("Проверить отображение приветственной страницы")
  public WelcomePage shouldBeOpened() {
    assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login").setExact(true)))
        .isVisible();
    return this;
  }

  @Step("Открыть страницу регистрации")
  public RegistrationPage openRegistration() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register").setExact(true)).click();
    return new RegistrationPage(page).shouldBeOpened();
  }

  @Step("Открыть страницу входа")
  public LoginPage openLogin() {
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login").setExact(true)).click();
    return new LoginPage(page).shouldBeOpened();
  }
}

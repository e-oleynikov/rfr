package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.model.TestUser;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LoginPage {

  private final Page page;
  private final Locator username;
  private final Locator password;

  public LoginPage(Page page) {
    this.page = page;
    username = page.getByPlaceholder("Type your username");
    password = page.getByPlaceholder("Type your password");
  }

  @Step("Проверить отображение формы входа")
  public LoginPage shouldBeOpened() {
    assertThat(username).isVisible();
    assertThat(password).isVisible();
    return this;
  }

  @Step("Войти под зарегистрированным пользователем")
  public TravelsPage signIn(TestUser user) {
    username.fill(user.username());
    password.fill(user.password());
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true)).click();
    return new TravelsPage(page).shouldBeOpened();
  }
}

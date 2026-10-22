package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.model.TestUser;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RegistrationPage {

  private final Page page;
  private final Locator username;
  private final Locator password;
  private final Locator passwordConfirmation;
  private final Locator signUp;

  public RegistrationPage(Page page) {
    this.page = page;
    username = page.getByPlaceholder("Type your username");
    password = page.getByPlaceholder("Type your password");
    passwordConfirmation = page.getByPlaceholder("Repeat your password");
    signUp = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up").setExact(true));
  }

  @Step("Проверить отображение формы регистрации")
  public RegistrationPage shouldBeOpened() {
    assertThat(username).isVisible();
    assertThat(password).isVisible();
    assertThat(passwordConfirmation).isVisible();
    assertThat(signUp).isVisible();
    return this;
  }

  @Step("Зарегистрировать пользователя")
  public RegistrationPage register(TestUser user) {
    username.fill(user.username());
    password.fill(user.password());
    passwordConfirmation.fill(user.password());
    signUp.click();
    return this;
  }

  @Step("Проверить успешную регистрацию")
  public RegistrationPage shouldBeRegistered() {
    assertThat(page.getByText("Congratulations! You've registered!", new Page.GetByTextOptions().setExact(true)))
        .isVisible();
    assertThat(signUp).hasCount(0);
    return this;
  }

  @Step("Вернуться в приложение по ссылке «Sign in»")
  public WelcomePage returnToApplication() {
    page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true)).click();
    return new WelcomePage(page).shouldBeOpened();
  }
}

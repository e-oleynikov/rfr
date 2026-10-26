package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.page.component.CredentialsForm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class LoginPage {

  public static String URL = TestConfig.authUrl() + "/login";

  private final Page page;
  private final CredentialsForm credentialsForm;
  private final Locator signInButton;
  private final Locator errorMessage;

  public LoginPage(Page page) {
    this.page = page;
    credentialsForm = new CredentialsForm(page);
    signInButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
    errorMessage = page.locator(".form__error");
  }

  @Step("Заполнить форму входа пользователя {user}")
  public LoginPage fillLoginForm(TestUser user) {
    credentialsForm.fillCredentials(user);
    return this;
  }

  @Step("Войти под зарегистрированным пользователем")
  public TravelsPage signIn() {
    clickSignIn();
    return new TravelsPage(page);
  }

  @Step("Отправить форму входа")
  public LoginPage clickSignIn() {
    signInButton.click();
    return this;
  }

  @Step("Проверить ошибку входа {message}")
  public LoginPage shouldHaveLoginError(String message) {
    assertThat(page).hasURL(URL + "?error");
    assertThat(errorMessage).hasText(message);
    return this;
  }
}

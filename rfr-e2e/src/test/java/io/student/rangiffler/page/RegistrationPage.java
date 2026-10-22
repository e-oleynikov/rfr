package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.page.component.CredentialsForm;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class RegistrationPage {

  public static String URL = TestConfig.authUrl() + "/register";

  private final Page page;
  private final CredentialsForm credentialsForm;
  private final Locator passwordConfirmation;
  private final Locator signUp;
  private final Locator successMessage;
  private final Locator signInLink;

  public RegistrationPage(Page page) {
    this.page = page;
    credentialsForm = new CredentialsForm(page);
    passwordConfirmation = page.getByPlaceholder("Repeat your password", new Page.GetByPlaceholderOptions().setExact(true));
    signUp = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up").setExact(true));
    successMessage = page.getByText("Congratulations! You've registered!", new Page.GetByTextOptions().setExact(true));
    signInLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
  }

  @Step("Заполнить форму регистрации пользователя {user}")
  public RegistrationPage fillRegistrationForm(TestUser user) {
    credentialsForm.fillCredentials(user);
    passwordConfirmation.fill(user.password());
    return this;
  }

  @Step("Отправить форму регистрации")
  public RegistrationPage submitRegistration() {
    signUp.click();
    return this;
  }

  @Step("Проверить успешную регистрацию")
  public RegistrationPage shouldBeRegistered() {
    assertThat(successMessage).isVisible();
    assertThat(signUp).hasCount(0);
    return this;
  }

  @Step("Вернуться в приложение по ссылке «Sign in»")
  public WelcomePage returnToApplication() {
    signInLink.click();
    return new WelcomePage(page);
  }
}

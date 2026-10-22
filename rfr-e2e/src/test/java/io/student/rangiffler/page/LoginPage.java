package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.page.component.CredentialsForm;

public final class LoginPage {

  public static String URL = TestConfig.authUrl() + "/login";

  private final Page page;
  private final CredentialsForm credentialsForm;
  private final Locator signInButton;

  public LoginPage(Page page) {
    this.page = page;
    credentialsForm = new CredentialsForm(page);
    signInButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
  }

  @Step("Заполнить форму входа пользователя {user}")
  public LoginPage fillLoginForm(TestUser user) {
    credentialsForm.fillCredentials(user);
    return this;
  }

  @Step("Войти под зарегистрированным пользователем")
  public TravelsPage signIn() {
    signInButton.click();
    return new TravelsPage(page);
  }
}

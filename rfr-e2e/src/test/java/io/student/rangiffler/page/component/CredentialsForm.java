package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.jupiter.fixture.TestUser;

public final class CredentialsForm {

  private final Locator username;
  private final Locator password;

  public CredentialsForm(Page page) {
    username = page.getByPlaceholder("Type your username", new Page.GetByPlaceholderOptions().setExact(true));
    password = page.getByPlaceholder("Type your password", new Page.GetByPlaceholderOptions().setExact(true));
  }

  @Step("Заполнить логин и пароль пользователя {user}")
  public CredentialsForm fillCredentials(TestUser user) {
    username.fill(user.username());
    password.fill(user.password());
    return this;
  }
}

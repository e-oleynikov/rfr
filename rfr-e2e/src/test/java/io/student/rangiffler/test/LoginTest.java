package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import io.qameta.allure.Feature;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.BrowserExtension;
import io.student.rangiffler.jupiter.extension.UserExtension;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Feature("Вход")
@ExtendWith({UserExtension.class, BrowserExtension.class})
public class LoginTest {

  @Test
  @DisplayName("Пользователь, созданный через @User, может войти в свой профиль")
  void shouldSignInWithPreparedUser(Page page, @User TestUser user) {
    new WelcomePage(page)
        .open()
        .openLogin()
        .fillLoginForm(user)
        .signIn()
        .openProfile()
        .shouldHaveUsername(user.username());
  }
}

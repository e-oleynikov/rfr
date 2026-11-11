package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUserRepository;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.BrowserExtension;
import io.student.rangiffler.jupiter.extension.UserExtension;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.page.WelcomePage;
import io.student.rangiffler.utils.RandomDataUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

@Feature("Вход")
@ExtendWith({UserExtension.class, BrowserExtension.class})
public class LoginNegativeTest {

  @BeforeEach
  void useEnglishAuthenticationMessages(Page page) {
    page.setExtraHTTPHeaders(Map.of("Accept-Language", "en-US"));
  }

  @Test
  @DisplayName("Неверный пароль существующего пользователя вызывает Bad credentials")
  void shouldRejectWrongPassword(Page page, @User TestUser user) {
    // Generated passwords are lowercase hexadecimal, so this value is always different.
    TestUser invalidCredentials = new TestUser(user.username(), "WrongPass!26");

    new WelcomePage(page)
        .open()
        .openLogin()
        .fillLoginForm(invalidCredentials)
        .clickSignIn()
        .shouldHaveLoginError("Bad credentials");
  }

  @Test
  @DisplayName("Несуществующий пользователь вызывает Bad credentials")
  void shouldRejectUnknownUser(Page page) {
    TestUser unknownUser = RandomDataUtils.randomUser();
    TestUserRepository repository = new TestUserRepository();
    repository.verifyUserAbsent(unknownUser.username());

    new WelcomePage(page)
        .open()
        .openLogin()
        .fillLoginForm(unknownUser)
        .clickSignIn()
        .shouldHaveLoginError("Bad credentials");

    repository.verifyUserAbsent(unknownUser.username());
  }
}

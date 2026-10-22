package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import io.qameta.allure.Feature;
import io.student.rangiffler.extension.BrowserExtension;
import io.student.rangiffler.extension.TestUserExtension;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Feature("Регистрация")
@ExtendWith({TestUserExtension.class, BrowserExtension.class})
public class RegistrationTest {

  @Test
  @DisplayName("Новый пользователь может зарегистрироваться и войти в свой профиль")
  void shouldRegisterAndSignIn(Page page, TestUser user) {
    new WelcomePage(page)
        .open()
        .openRegistration()
        .register(user)
        .shouldBeRegistered()
        .returnToApplication()
        .openLogin()
        .signIn(user)
        .openProfile()
        .shouldHaveUsername(user.username());
  }
}

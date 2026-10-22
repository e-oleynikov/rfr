package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import io.qameta.allure.Feature;
import io.student.rangiffler.jupiter.extension.BrowserExtension;
import io.student.rangiffler.data.TestUserRepository;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.page.WelcomePage;
import io.student.rangiffler.utils.RandomDataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Feature("Регистрация")
@ExtendWith(BrowserExtension.class)
public class RegistrationTest {

  private TestUserRepository userRepository;
  private TestUser user;

  @BeforeEach
  void prepareRegistrationData() {
    userRepository = new TestUserRepository();
    TestUser candidate = RandomDataUtils.randomUser();
    userRepository.verifyUserAbsent(candidate.username());
    user = candidate;
  }

  @AfterEach
  void deleteRegisteredUser() {
    if (user != null) {
      userRepository.delete(user.username());
    }
  }

  @Test
  @DisplayName("Новый пользователь может зарегистрироваться и войти в свой профиль")
  void shouldRegisterAndSignIn(Page page) {
    new WelcomePage(page)
        .open()
        .openRegistration()
        .fillRegistrationForm(user)
        .submitRegistration()
        .shouldBeRegistered()
        .returnToApplication()
        .openLogin()
        .fillLoginForm(user)
        .signIn()
        .openProfile()
        .shouldHaveUsername(user.username());
  }
}

package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import io.qameta.allure.Feature;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.BrowserExtension;
import io.student.rangiffler.jupiter.extension.TestUserExtension;
import io.student.rangiffler.jupiter.fixture.TestUser;
import io.student.rangiffler.page.WelcomePage;
import io.student.rangiffler.ui.page.ProfilePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Feature("Профиль")
@ExtendWith({TestUserExtension.class, BrowserExtension.class})
public class ProfileTest {

  private static final String SAVED_COUNTRY = "Germany";
  private static final String CHANGED_COUNTRY = "France";
  private static final String DEFAULT_COUNTRY = "Russian Federation";

  @Test
  @DisplayName("Имя сохраняется после перезагрузки профиля")
  void shouldKeepFirstNameAfterReload(Page page, @User TestUser user) {
    openUserProfile(page, user)
        .fillProfileDetails("Elena", "")
        .clickSave()
        .shouldShowSavedConfirmation()
        .reload()
        .shouldHaveProfileDetails("Elena", "")
        .shouldHaveCountry(DEFAULT_COUNTRY)
        .shouldHaveUsername(user.username());
  }

  @Test
  @DisplayName("Фамилия сохраняется после перезагрузки профиля")
  void shouldKeepSurnameAfterReload(Page page, @User TestUser user) {
    openUserProfile(page, user)
        .fillProfileDetails("", "Ivanova")
        .clickSave()
        .shouldShowSavedConfirmation()
        .reload()
        .shouldHaveProfileDetails("", "Ivanova")
        .shouldHaveCountry(DEFAULT_COUNTRY)
        .shouldHaveUsername(user.username());
  }

  @Test
  @DisplayName("Выбранная страна Germany сохраняется после перезагрузки профиля")
  void shouldKeepCountryAfterReload(Page page, @User TestUser user) {
    openUserProfile(page, user)
        .selectCountry(SAVED_COUNTRY)
        .clickSave()
        .shouldShowSavedConfirmation()
        .reload()
        .shouldHaveCountry(SAVED_COUNTRY)
        .shouldHaveProfileDetails("", "")
        .shouldHaveUsername(user.username());
  }

  @Test
  @DisplayName("Загруженный аватар сохраняется после перезагрузки профиля")
  void shouldKeepAvatarAfterReload(Page page, @User TestUser user) throws IOException {
    Path avatar = imageResource("avatar-blue.png");
    String expectedAvatar = imageDataUrl(avatar);

    openUserProfile(page, user)
        .uploadAvatar(avatar)
        .shouldHaveAvatar(expectedAvatar)
        .clickSave()
        .shouldShowSavedConfirmation()
        .reload()
        .shouldHaveAvatar(expectedAvatar)
        .shouldHaveProfileDetails("", "")
        .shouldHaveCountry(DEFAULT_COUNTRY)
        .shouldHaveUsername(user.username());
  }

  @Test
  @DisplayName("Reset возвращает сохранённые имя, фамилию, страну и аватар")
  void shouldResetToSavedProfile(Page page, @User TestUser user) throws IOException {
    Path savedAvatar = imageResource("avatar-blue.png");
    Path changedAvatar = imageResource("avatar-orange.png");
    String savedDataUrl = imageDataUrl(savedAvatar);
    String changedDataUrl = imageDataUrl(changedAvatar);

    openUserProfile(page, user)
        .fillProfileDetails("Anna", "Petrova")
        .selectCountry(SAVED_COUNTRY)
        .uploadAvatar(savedAvatar)
        .shouldHaveAvatar(savedDataUrl)
        .clickSave()
        .shouldShowSavedConfirmation()
        .reload()
        .shouldHaveProfileDetails("Anna", "Petrova")
        .shouldHaveCountry(SAVED_COUNTRY)
        .shouldHaveAvatar(savedDataUrl)
        .fillProfileDetails("Maria", "Sidorova")
        .selectCountry(CHANGED_COUNTRY)
        .uploadAvatar(changedAvatar)
        .shouldHaveProfileDetails("Maria", "Sidorova")
        .shouldHaveCountry(CHANGED_COUNTRY)
        .shouldHaveAvatar(changedDataUrl)
        .clickReset()
        .shouldHaveProfileDetails("Anna", "Petrova")
        .shouldHaveCountry(SAVED_COUNTRY)
        .shouldHaveAvatar(savedDataUrl)
        .reload()
        .shouldHaveProfileDetails("Anna", "Petrova")
        .shouldHaveCountry(SAVED_COUNTRY)
        .shouldHaveAvatar(savedDataUrl)
        .shouldHaveUsername(user.username());
  }

  private ProfilePage openUserProfile(Page page, TestUser user) {
    return new WelcomePage(page)
        .open()
        .openLogin()
        .fillLoginForm(user)
        .signIn()
        .openProfile()
        .shouldHaveUsername(user.username());
  }

  private Path imageResource(String filename) throws IOException {
    return new ClassPathResource("images/" + filename).getFile().toPath();
  }

  private String imageDataUrl(Path image) throws IOException {
    return "data:image/png;base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(image));
  }
}

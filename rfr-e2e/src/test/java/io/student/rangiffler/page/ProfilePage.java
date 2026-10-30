package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

import java.nio.file.Path;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class ProfilePage {

  public static String URL = TestConfig.frontUrl() + "/profile";

  private static final Pattern UPDATE_USER_OPERATION =
      Pattern.compile("\"operationName\"\\s*:\\s*\"UpdateUser\"");

  private final Page page;
  private final Locator heading;
  private final Locator usernameInput;
  private final Locator firstNameInput;
  private final Locator surnameInput;
  private final Locator locationSelect;
  private final Locator countryOptions;
  private final Locator avatarInput;
  private final Locator avatarImage;
  private final Locator saveButton;
  private final Locator resetButton;
  private final Locator savedConfirmation;

  public ProfilePage(Page page) {
    this.page = page;
    heading = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("My profile").setExact(true));
    usernameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username").setExact(true));
    firstNameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("First Name").setExact(true));
    surnameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Surname").setExact(true));
    locationSelect = page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Location").setExact(true));
    countryOptions = page.getByRole(AriaRole.OPTION);
    avatarInput = page.getByRole(AriaRole.MAIN).locator("input[type='file']");
    avatarImage = page.getByRole(AriaRole.MAIN).locator(".MuiAvatar-root img[src^='data:image/']");
    saveButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save").setExact(true));
    resetButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset").setExact(true));
    savedConfirmation = page.getByRole(AriaRole.ALERT)
        .filter(new Locator.FilterOptions().setHasText("Your profile is successfully updated"));
  }

  @Step("Заполнить профиль: имя {firstName}, фамилия {surname}")
  public ProfilePage fillProfileDetails(String firstName, String surname) {
    firstNameInput.fill(firstName);
    surnameInput.fill(surname);
    return this;
  }

  @Step("Выбрать страну проживания {country}")
  public ProfilePage selectCountry(String country) {
    locationSelect.click();
    countryOptions.filter(new Locator.FilterOptions().setHas(
        page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(country).setExact(true)))).click();
    return this;
  }

  @Step("Загрузить аватар из файла {image}")
  public ProfilePage uploadAvatar(Path image) {
    avatarInput.setInputFiles(image);
    return this;
  }

  @Step("Сохранить профиль и дождаться ответа UpdateUser")
  public ProfilePage clickSave() {
    page.waitForResponse(response -> {
      String body = response.request().postData();
      return response.url().endsWith("/graphql")
          && "POST".equals(response.request().method())
          && body != null && UPDATE_USER_OPERATION.matcher(body).find();
    }, saveButton::click).finished();
    return this;
  }

  @Step("Сбросить несохранённые изменения профиля")
  public ProfilePage clickReset() {
    resetButton.click();
    return this;
  }

  @Step("Перезагрузить страницу профиля")
  public ProfilePage reload() {
    page.reload();
    return this;
  }

  @Step("Проверить сохранение профиля по уведомлению")
  public ProfilePage shouldShowSavedConfirmation() {
    assertThat(savedConfirmation).isVisible();
    return this;
  }

  @Step("Проверить имя {firstName} и фамилию {surname} в профиле")
  public ProfilePage shouldHaveProfileDetails(String firstName, String surname) {
    assertThat(firstNameInput).hasValue(firstName);
    assertThat(surnameInput).hasValue(surname);
    return this;
  }

  @Step("Проверить выбранную страну {country}")
  public ProfilePage shouldHaveCountry(String country) {
    assertThat(locationSelect).hasText(country);
    return this;
  }

  @Step("Проверить аватар с содержимым {dataUrl}")
  public ProfilePage shouldHaveAvatar(String dataUrl) {
    assertThat(avatarImage).isVisible();
    assertThat(avatarImage).hasAttribute("src", dataUrl);
    return this;
  }

  @Step("Проверить, что профиль принадлежит пользователю {username}")
  public ProfilePage shouldHaveUsername(String username) {
    assertThat(page).hasURL(URL);
    assertThat(heading).isVisible();
    assertThat(usernameInput).hasValue(username);
    return this;
  }
}

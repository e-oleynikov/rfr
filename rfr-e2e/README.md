# UI-тесты

Требуются JDK 25, MySQL и запущенные `rfr-auth`, `rfr-api`, `rfr-gql-client`.

Перед запуском задайте пароль локальной MySQL в переменной окружения
`RFR_DB_PASSWORD`. В zsh можно ввести его без отображения и сохранения в истории:

```zsh
read -rs 'RFR_DB_PASSWORD?MySQL password: '
export RFR_DB_PASSWORD
```

Из корня репозитория:

```bash
./gradlew :rfr-e2e:test --tests '*RegistrationTest'
```

`RegistrationTest` регистрирует уникального пользователя через UI,
входит с его данными и проверяет логин в профиле. Данные создаёт `RandomDataUtils`,
очистку выполняет `@AfterEach`; до регистрации учётной записи в БД нет.

`LoginTest` проверяет вход с заранее созданной учётной записью:

```bash
./gradlew :rfr-e2e:test --tests '*LoginTest'
```

`LoginNegativeTest` проверяет неверный пароль и несуществующего пользователя.
В обоих случаях ожидается `Bad credentials` в `.form__error` на странице входа.
Для детерминированного текста Spring Security тесты задают `Accept-Language: en-US`.
Второй сценарий не создаёт пользователя и проверяет отсутствие записи в БД.

```bash
./gradlew :rfr-e2e:test --tests '*LoginNegativeTest'
```

Тестовый метод принимает `@User TestUser user`. На классе нужно зарегистрировать
`@ExtendWith({TestUserExtension.class, BrowserExtension.class})`.
`@User` всегда означает создание пользователя в auth-БД, без режима «только данные».

`ProfileTest` проверяет сохранение имени, фамилии, страны Germany и аватара после
перезагрузки, а также возврат сохранённых значений по Reset. Страны фиксированы,
изображения находятся в `src/test/resources/images/`. Каждый сценарий использует
отдельного пользователя через `@User`.

```bash
./gradlew :rfr-e2e:test --tests '*ProfileTest'
```

`ui/page/ProfilePage` отделяет заполнение данных, выбор страны, загрузку аватара,
Save, Reset и проверки. `clickSave()` дожидается ответа GraphQL `UpdateUser`,
чтобы последующий reload не опередил сохранение.

Page Object скрывают локаторы, действия и ожидающие проверки Playwright.
`BrowserExtension` передаёт тесту `Page` с отдельным браузерным контекстом.
`TestUserExtension` обрабатывает только параметры `TestUser` с аннотацией `@User`.
Для каждого параметра сохраняется отдельный `UserFixture implements AutoCloseable`
в `ExtensionContext.Store` тестового метода. `UserFixture.create()` создаёт
пользователя и права `read`/`write` в auth-БД, расширение передаёт данные в параметр.
После теста и закрытия браузера JUnit 6 автоматически вызывает `UserFixture.close()`,
который удаляет учётную запись и API-профиль. Ручного `afterEach` в расширении нет.
Фикстура сохраняется до вызова `create()`, поэтому закрывается и при ошибке подготовки.
Повторный `close()` после успешного удаления ничего не делает.
Несколько параметров `@User` получают разные фикстуры и учётные записи.
Не отключайте настройку JUnit `junit.jupiter.extensions.store.close.autocloseable.enabled`.
Пароль кодируется тем же `PasswordEncoderFactories.createDelegatingPasswordEncoder()`,
что и в auth-сервисе. API-профиль создаёт само приложение при первом входе.
Создание и очистка находятся в `TestUserRepository`.

API удаления пользователя отсутствует, поэтому очистка работает через Spring JDBC
без запуска Spring-контекста. В пакете `data/db`:

- `Databases.AUTH` и `Databases.API` содержат собственные `DataSource` и transaction manager.
  Оба используют настройки MySQL из `TestConfig`, выбирая соответствующий каталог.
- `Jdbc.getClient(database)` возвращает Spring `JdbcClient`.
- `Jdbc.inTransaction(database, client -> { ... })` выполняет действия в транзакции
  выбранной БД с автоматическим rollback при ошибке.
- `Jdbc.loadSql(resourcePath)` читает SQL из classpath в UTF-8.

Все запросы находятся в `src/test/resources/sql/`, параметры передаются по именам.
Очистка выполняется через один клиент `AUTH`, а SQL явно указывает обе схемы:
это сохраняет общую транзакцию на одном соединении. Переключение на клиент другой
БД внутри callback не включит его в ту же локальную транзакцию.

При очистке
профиль в `rangiffler-api`, права и пользователь в `rangiffler-auth` удаляются
в одной транзакции. Нужен доступ к обеим схемам на одном сервере MySQL.
Удаляется только точный логин текущего запуска с префиксом `ui_registration_`.
До регистрации проверяются доступ к БД и отсутствие такого логина;
после удаления проверяется отсутствие пользователя в обеих схемах.
Ошибка очистки приводит к неуспешному результату теста.

`RandomDataUtils` генерирует уникальный логин, пароль и `TestUser` средствами
стандартной Java, без дополнительных библиотек. Модель `TestUser` хранит данные.

Все настройки читает `config/TestConfig`: system property → переменная окружения →
константа fallback. Например, `rfr.front.url` → `RFR_FRONT_URL` → `http://localhost:3001`.
Префикс system properties — `rfr.`, ENV — `RFR_`; точки в ключах заменяются на `_`.
Gradle передаёт свойства `rfr.*` в тестовую JVM; тот же формат работает при запуске из IDEA.

| System property | ENV | Fallback |
| --- | --- | --- |
| `rfr.front.url` | `RFR_FRONT_URL` | `http://localhost:3001` |
| `rfr.auth.url` | `RFR_AUTH_URL` | `http://localhost:9001` |
| `rfr.headed` | `RFR_HEADED` | `false`; `true` открывает видимый Chromium |
| `rfr.db.url` | `RFR_DB_URL` | MySQL на `localhost:3306`, см. `TestConfig` |
| `rfr.db.user` | `RFR_DB_USER` | `root` |
| `rfr.db.password` | `RFR_DB_PASSWORD` | Обязательная, fallback отсутствует |

Пример запуска с system property:

```bash
./gradlew :rfr-e2e:test --tests '*RegistrationTest' -Drfr.front.url=http://localhost:3001
```

Chromium должен соответствовать версии Playwright из `build.gradle`;
при первом запуске Playwright загружает недостающие браузеры автоматически.

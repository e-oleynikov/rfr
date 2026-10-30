# Модуль rfr-api

Общие правила находятся в [корневом AGENTS.md](../AGENTS.md), подробности API — в [README.md](README.md).

## Назначение и структура

Монолитный GraphQL API на Spring Boot, локальный порт `8081`, endpoint `/graphql`, GraphiQL — `/graphiql`. Java-код находится в `src/main/java/io/student/rangiffler/`: контроллеры, сервисы, репозитории и JPA-сущности.

Конфигурация находится в `src/main/resources/application.yml`. БД — MySQL, схема `rangiffler-api`; миграции — в `src/main/resources/db/migration/`.

## Контракт и моки

- Источник GraphQL-контракта — `src/main/resources/graphql/query.graphqls`. Типы Java создаёт DGS Codegen задачей `generateJava` в `build/generated/sources/dgs-codegen`; не редактируйте их вручную.
- При изменении контракта проверяйте связанные операции в `rfr-gql-client/src/hooks/` и типы в `rfr-gql-client/src/types/`.
- Пользователи, дружба и страны используют БД. `FeedMockQueryController`, `PhotoMockMutationController` и `service/mock/FeedMockData` обслуживают моки фотографий, лайков и статистики из `src/main/resources/mock/`.
- Успешный ответ мутации фотографии не означает сохранение в БД. При проверке учитывайте повторное чтение данных.
- Сохраняйте единый контракт ошибок через `GraphQlExceptionResolver` и проверки доступа GraphQL-операций. API принимает access token от `rfr-auth`.
- При проверке ответа GraphQL учитывайте поле `errors`: HTTP 200 не гарантирует успешное выполнение операции. Невалидный Bearer-токен может быть отклонён фильтрами с HTTP 401 до выполнения GraphQL.
- UUID пользователя в auth-БД и API-БД независимы; учётные записи связаны по username. Не используйте auth UUID вместо API UUID в операциях API.

## Команды из корня репозитория

- `./gradlew :rfr-api:generateJava` — генерация типов по схеме.
- `./gradlew :rfr-api:bootRun` — запуск API.
- `./gradlew :rfr-api:build` — сборка и настроенные проверки модуля.

Для проверки авторизованных запросов нужны MySQL и запущенный `rfr-auth`.

# ☁️ Cloud File Storage

Многопользовательское облачное хранилище файлов, вдохновлённое Google Drive.
Пользователи могут регистрироваться, загружать файлы и папки, скачивать, переименовывать, перемещать, искать и удалять их.

Проект выполнен в рамках [Java Backend Learning Course](https://zhukovsd.github.io/java-backend-learning-course/projects/cloud-file-storage/) Сергея Жукова.

---

## Содержание
- [О проекте](#о-проекте)
- [Деплой](#деплой)
- [Локальный запуск](#локальный-запуск)
- [Технологический стек](#технологический-стек)
- [Функциональность](#функциональность)
- [Структура проекта](#структура-проекта)
- [Хранилища данных](#хранилища-данных)
- [REST API](#-rest-api)
- [Тесты](#тесты)

---

## О проекте

Шестой проект роадмапа. Основная цель — научиться работать сразу с тремя хранилищами (PostgreSQL, Redis, MinIO) и грамотно разделить ответственность между ними. Здесь впервые используется Spring Security с сессионной авторизацией через Redis.

---

## Деплой
Проект задеплоен на VPS:
+ [Frontend](http://31.77.9.144)
+ [Swagger UI](http://31.77.9.144/swagger-ui/index.html)

**Тестовый пользователь:**
+ username: reviewer
+ password: qwerty123

---

## Локальный запуск

### Требования
- Docker и Docker Compose

### Шаги

**1. Клонировать репозиторий**
```bash
git clone https://github.com/XanderGI/CloudFileStorage.git
cd CloudFileStorage
```

**2. Создать файл `.env`** в корне проекта со следующими переменными:
```env
DB_USERNAME=test
DB_PASSWORD=pass

REDIS_PASSWORD=pass

MINIO_USER=test
MINIO_PASSWORD=pass
```

**3. Указать адрес бэкенда для фронтенда** в файле `deploy/config.js`:
```js
// для локального запуска:
baseUrl: "http://localhost:8080"
````

```js
// ИЛИ для развертывания на VPS
baseUrl: "http://<YOUR_SERVER_IP>:8080" 
```

**4. Запустить стек через Docker Compose:**

Для dev формата (все порты сервисов открыты):
```bash
docker compose -f docker-compose.yaml -f docker-compose.dev.yaml up -d --build
```
Для prod формата (наружу открыт только порт 80):
```bash
docker compose -f docker-compose.yaml -f docker-compose.prod.yaml up -d --build
```

### Адреса после запуска (dev-формат)

| Сервис               | Адрес                                       |
|----------------------|---------------------------------------------|
| Frontend             | http://localhost:8081                       |
| Backend API          | http://localhost:8080                       |
| Swagger UI           | http://localhost:8080/swagger-ui/index.html |
| MinIO Console Web UI | http://localhost:9001                       |
| PostgreSQL           | http://localhost:5430                       |
| Redis                | http://localhost:6379                       |

---

## Технологический стек

| Категория             | Технологии                                                                                      |
|-----------------------|-------------------------------------------------------------------------------------------------|
| **Язык / Платформа**  | Java 17, Spring Boot 4.1                                                                        |
| **Web / API**         | Spring Web MVC, REST, Springdoc OpenAPI (Swagger UI 3.1)                                        |
| **Безопасность**      | Spring Security, Spring Session Data Redis                                                      |
| **Базы данных**       | PostgreSQL, Spring Data JPA, Hibernate                                                          |
| **Миграции**          | Liquibase                                                                                       |
| **Файловое хранилище**| MinIO (S3-совместимое), Minio Java SDK 9.0.3                                                    |
| **Сессии**            | Redis (через Spring Session)                                                                    |
| **Утилиты**           | Lombok, Bean Validation (Jakarta)                                                               |
| **Сборка**            | Gradle (Kotlin DSL)                                                                             |
| **Инфраструктура**    | Docker, Docker Compose, Nginx                                                                   |
| **Тесты**             | JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL + MinIO)                                  |
| **Фронтенд**          | [Готовый React SPA](https://github.com/zhukovsd/cloud-storage-frontend) (раздаётся через Nginx) |

---

## Функциональность

### Аутентификация и пользователи
- Регистрация нового пользователя (username + password)
- Авторизация по сессии (сессия хранится в Redis, выставляется cookie)
- Выход из аккаунта (logout с инвалидацией сессии)
- Получение информации о текущем авторизованном пользователе (`GET /api/user/me`)

### Работа с файлами и папками
> **Ресурс** — файл или папка. Путь к папке всегда заканчивается на `/`.

- **Загрузка** — один или несколько файлов / целая папка с вложенными подпапками одним запросом (через `multipart/form-data`)
- **Просмотр** — получение содержимого папки (нерекурсивно)
- **Скачивание** — файл отдаётся как `application/octet-stream`; папка упаковывается в `.zip`
- **Создание папки** — создание новой пустой директории по указанному пути
- **Переименование / перемещение** — единый эндпоинт, определяет операцию по изменению имени или пути
- **Удаление** — рекурсивное удаление файла или папки со всем содержимым
- **Поиск** — поиск по имени ресурса в пределах хранилища текущего пользователя

### Изоляция данных
Каждый пользователь работает только со своими файлами.
В MinIO под каждого пользователя создаётся отдельная директория `user-{id}-files/` внутри бакета `user-files`.

---

## Структура проекта

```
CloudFileStorage/
├── deploy/
│   ├── config.js          # Конфигурация фронтенда (baseUrl API, правила валидации)
│   └── nginx.conf         # Конфиг Nginx для раздачи React SPA
├── http-tests/
│   ├── directoryOperations.http   # Ручное тестирование Directory API
│   ├── resourceOperations.http    # Ручное тестирование Resource API
│   └── userRequest.http           # Ручное тестирование Auth/User API
└── src/
    ├── main/
    │   ├── java/io/github/XanderGI/
    │   │   ├── config/
    │   │   │   ├── minio/
    │   │   │   │   ├── MinioBucketInitializer   # Инициализация бакета при старте
    │   │   │   │   ├── MinioClientConfig        # Бин MinioClient
    │   │   │   │   └── MinioProperties          # Параметры подключения к MinIO
    │   │   │   └── OpenApiConfig                # Конфигурация Swagger / OpenAPI
    │   │   ├── constraint/                      # Кастомные аннотации валидации путей
    │   │   │   ├── AnyDirectoryPath
    │   │   │   ├── AnyResourcePath
    │   │   │   ├── NonRootDirectoryPath
    │   │   │   ├── NonRootResourcePath
    │   │   │   └── UploadTargetPath
    │   │   ├── controller/
    │   │   │   ├── advice/
    │   │   │   │   └── GlobalExceptionHandler   # @RestControllerAdvice, обработка ошибок
    │   │   │   ├── impl/                        # Реализации контроллеров
    │   │   │   │   ├── AuthController
    │   │   │   │   ├── DirectoryController
    │   │   │   │   ├── ResourceController
    │   │   │   │   └── UserController
    │   │   │   ├── AuthControllerApi            # Swagger-интерфейс для Auth
    │   │   │   ├── DirectoryControllerApi       # Swagger-интерфейс для Directory
    │   │   │   ├── ResourceControllerApi        # Swagger-интерфейс для Resource
    │   │   │   ├── ResourceEndpointResponses    # Аннотация для описания ответов в Swagger
    │   │   │   └── UserControllerApi            # Swagger-интерфейс для User
    │   │   ├── dto/
    │   │   │   ├── internal/                    # Внутренние DTO (не выходят наружу)
    │   │   │   │   ├── DownloadResult
    │   │   │   │   ├── ResourceStream
    │   │   │   │   └── UploadFileItem
    │   │   │   ├── request/                     # DTO входящих запросов
    │   │   │   │   ├── CreateDirectoryRequestDto
    │   │   │   │   ├── DirectoryPathRequestDto
    │   │   │   │   ├── MoveResourceRequestDto
    │   │   │   │   ├── ResourcePathRequestDto
    │   │   │   │   ├── SearchRequestDto
    │   │   │   │   ├── UploadResourceRequestDto
    │   │   │   │   └── UserRequestDto
    │   │   │   └── response/                    # DTO ответов
    │   │   │       ├── ErrorResponseDto
    │   │   │       ├── ResourceResponseDto
    │   │   │       └── ResourceType             # Enum: FILE / DIRECTORY
    │   │   ├── entity/                          # JPA-сущности (User)
    │   │   ├── exception/                       # Кастомные исключения
    │   │   ├── mapper/                          # Ручной маппинг DTO 
    │   │   ├── repository/                      # Spring Data JPA репозитории
    │   │   ├── security/                        # Spring Security (UserDetails, конфигурация)
    │   │   ├── service/                         # Бизнес-логика (UserService, ResourceService, ...)
    │   │   └── storage/
    │   │       ├── impl/
    │   │       │   └── MinioStorageClient       # Реализация работы с MinIO
    │   │       ├── StorageClient                # Интерфейс файлового хранилища
    │   │       ├── StorageItem                  # dto модели объектов из MinIO
    │   │       └── StorageObjectInfo            
    │   └── resources/
    │       ├── db/changelog/                    # Liquibase changeset'ы
    │       ├── application.properties           
    │       ├── application-dev.properties       
    │       └── application-prod.properties      
    └── test/
        ├── java/io/github/XanderGI/
        │   ├── controller/                      # Тесты контроллеров (MockMvc)
        │   ├── integration/                     # Сквозные интеграционные тесты
        │   ├── mapper/                          
        │   ├── service/                         
        │   ├── CloudFileStorageApplicationTests
        │   ├── TestCloudFileStorageApplication  
        │   └── TestcontainersConfiguration      # Общий Testcontainers конфиг (Postgre + redis + MinIO)
        └── resources/
            ├── test-files/                      # Тестовые файлы для upload-тестов (юзаются в resourceOperations.http)
            └── application-test.properties
```

---

## Хранилища данных

В проекте используется три независимых хранилища, каждое под свою зону ответственности:

| Хранилище      | Назначение                                         | Технология                  |
|----------------|----------------------------------------------------|-----------------------------|
| **PostgreSQL**  | Пользователи (таблица `users`)                     | Spring Data JPA + Liquibase |
| **Redis**       | Сессии авторизованных пользователей (с TTL)        | Spring Session Data Redis   |
| **MinIO (S3)**  | Файлы и папки пользователей                        | Minio Java SDK              |

**Структура бакета MinIO:**
```
user-files/               ← единственный бакет
├── user-1-files/         ← корневая директория пользователя с id=1
│   ├── docs/
│   │   └── report.pdf
│   └── photo.jpg
└── user-2-files/
    └── ...
```

---

## 📡 REST API

Все эндпоинты расположены под префиксом `/api`. Авторизация — через сессионную cookie.
Полная интерактивная документация доступна через **Swagger UI**: `http://31.77.9.144/swagger-ui/index.html`

### Аутентификация

| Метод  | Путь              | Тело запроса            | Успех       | Описание              |
|--------|-------------------|-------------------------|-------------|-----------------------|
| `POST` | `/auth/sign-up`   | `{username, password}`  | `201`       | Регистрация           |
| `POST` | `/auth/sign-in`   | `{username, password}`  | `200`       | Авторизация           |
| `POST` | `/auth/sign-out`  | —                       | `204`       | Выход из аккаунта     |

### Пользователь

| Метод | Путь        | Успех | Описание                        |
|-------|-------------|-------|---------------------------------|
| `GET` | `/user/me`  | `200` | Данные текущего пользователя    |

### Ресурсы (файлы и папки)

| Метод    | Путь                         | Параметры              | Успех | Описание                              |
|----------|------------------------------|------------------------|-------|---------------------------------------|
| `GET`    | `/resource`                  | `?path=`               | `200` | Информация о ресурсе                  |
| `POST`   | `/resource`                  | `?path=` + multipart   | `201` | Загрузка файлов / папок               |
| `DELETE` | `/resource`                  | `?path=`               | `204` | Удаление ресурса                      |
| `GET`    | `/resource/download`         | `?path=`               | `200` | Скачивание (папка → `.zip`)           |
| `POST`   | `/resource/move`             | `?from=&to=`           | `200` | Переименование / перемещение          |
| `GET`    | `/resource/search`           | `?query=`              | `200` | Поиск по имени                        |

### Директории

| Метод  | Путь          | Параметры | Успех | Описание                           |
|--------|---------------|-----------|-------|------------------------------------|
| `GET`  | `/directory`  | `?path=`  | `200` | Содержимое папки (нерекурсивно)    |
| `POST` | `/directory`  | `?path=`  | `201` | Создание новой пустой папки        |

---
## Тесты

Проект покрыт **интеграционными тестами** с использованием реальных Docker-контейнеров через **Testcontainers** (Postgres + Redis + MinIO).

Общая конфигурация контейнеров вынесена в `TestcontainersConfiguration`.

### Что покрыто

| Слой            | Пакет           | Что тестируется                                                                          |
|-----------------|-----------------|------------------------------------------------------------------------------------------|
| **Controller**  | `controller/`   | HTTP-слой через MockMvc (статусы, тела ответов, валидация) **только для AuthController** |
| **Service**     | `service/`      | Бизнес-логика: `AuthService`, `ResourceService` и unit-тесты для `MinioPathHelper`       |
| **Integration** | `integration/`  | Сквозные сценарии для sign-up и валидация cookies у SESSION поля                         |
| **Mapper**      | `mapper/`       | Корректность логики работы при возникновении `IOException` в процессе операции upload    |

> Ручное тестирование API выполнялось через HTTP-клиент IntelliJ IDEA.
> Файлы запросов находятся в директории [`http-tests/`](./http-tests):
> - [`userRequest.http`](./http-tests/userRequest.http) — Auth и User эндпоинты
> - [`resourceOperations.http`](./http-tests/resourceOperations.http) — Resource эндпоинты
> - [`directoryOperations.http`](./http-tests/directoryOperations.http) — Directory эндпоинты

### Запуск тестов
```bash
./gradlew test
```

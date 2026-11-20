# True Social — Social Media REST API

A social media backend built with **Java 17** and **Spring Boot 3.5**.

Users can:

- register and log in with **JWT authentication**
- publish, edit and delete posts
- comment on posts
- like posts
- follow other users and read a personalized feed
- receive in-app notifications

The project focuses on solid backend fundamentals: a layered architecture, DTOs, validation, consistent error responses, ownership-based authorization, and automated tests.

---

## Tech stack

| Area           | Technology                                                  |
|----------------|-------------------------------------------------------------|
| Language       | Java 17                                                     |
| Framework      | Spring Boot 3.5 (Spring Web, Spring Data JPA, Validation)   |
| Security       | Spring Security 6, stateless JWT (jjwt 0.11), BCrypt        |
| Database       | MySQL 8 (Hibernate / JPA)                                   |
| File storage   | Cloudinary (profile photos, optional)                       |
| Build          | Maven (wrapper included)                                    |
| Testing        | JUnit 5, Mockito, AssertJ, Spring MockMvc, H2 (in-memory)   |
| Other          | Lombok                                                      |

---

## Features

**Authentication & account**
- Register and log in. Both return a signed JWT access token that expires.
- Passwords are hashed with BCrypt. Emails are trimmed and lower-cased.
- View and update your own profile (name, email, bio).
- Change your password. The current password is required.
- Upload a profile photo to Cloudinary. Only JPEG, PNG, WEBP and GIF files up to 5 MB are accepted.

**Users**
- Public profile with follower, following and post counts, plus whether you follow the user.
- Emails stay private: they are only returned when you view your own profile.
- Search users by name (case-insensitive) with pagination.

**Posts**
- Create, read, update (partial) and delete posts.
- Only the author or an admin can edit or delete a post.
- Paginated and sortable list of all posts, the posts of a user, and a **feed**. The feed contains your own posts plus posts from the users you follow.
- Each post includes its like count, its comment count, and whether you liked it.

**Likes, comments, follows**
- Like and unlike posts. A user can like a post only once, enforced by a database unique constraint.
- Comment CRUD with pagination.
  - The comment author edits their comment.
  - The comment author, the post author or an admin can delete it.
- Follow and unfollow users. Paginated followers and following lists. You can't follow yourself.

**Notifications**
- A notification is created when someone likes your post, comments on it, or follows you. Your own actions never notify you.
- List all notifications or only unread ones, get the unread count, mark one as read, or mark all as read.

**Administration**
- Roles `ROLE_USER` and `ROLE_ADMIN` are created automatically at startup.
- `/api/roles` is restricted to admins with `@PreAuthorize`.

---

## Architecture

The code is a classic layered Spring Boot application, organized **by feature**:

```
Controller  ->  Service  ->  Repository  ->  MySQL
  (HTTP, DTOs,    (business rules,    (Spring Data JPA)
   validation)     transactions,
                   authorization)
```

```
src/main/java/com/example/Social_Media_RestApi_Project
├── auth/            register & login (controller, service, DTOs)
├── user/            profiles, search, password, profile photo
├── post/            posts, likes, feed (entities Post & PostLike)
├── comment/         comments on posts
├── follow/          follow / unfollow, followers & following lists
├── notification/    in-app notifications
├── role/            roles + startup data initializer (admin-only API)
├── security/        SecurityConfig, JWT service & filter, UserPrincipal, CurrentUserProvider
├── storage/         Cloudinary image upload
├── config/          Cloudinary configuration properties
├── exception/       custom exceptions + GlobalExceptionHandler (RFC 7807 problem details)
└── common/          BaseEntity (id + audit timestamps), PageResponse, SortValidator
```

Key design decisions:

- **DTOs everywhere.** Entities are never serialized directly, so password hashes and private emails can't leak. Request and response DTOs are Java `record`s.
- **Identity comes from the token, never from the request body.** The author of a post or comment is always the authenticated user. `CurrentUserProvider.canModify(ownerId)` centralizes the "owner or admin" rule.
- **Consistent errors.** Every error is an RFC 7807 `application/problem+json` body, including the 401 and 403 responses produced by Spring Security.
- **No N+1 queries on lists.** Post pages fetch authors with `@EntityGraph`. Like counts, comment counts and "liked by me" are loaded with one aggregate query each for the whole page.
- **Stable pagination contract.** Lists return a `PageResponse` (`content, page, size, totalElements, totalPages, first, last`). Page size is capped at 50. Sorting is limited to an allow-list of properties.
- **Stateless security.** No HTTP session. The JWT subject is the user id, so changing your email doesn't invalidate your token.
- **Secrets from the environment.** The JWT secret, database credentials and Cloudinary keys are not stored in the repository.

---

## Getting started

### Prerequisites

- JDK 17 or newer
- MySQL 8 running locally (or anywhere reachable)

Maven doesn't need to be installed, because the `./mvnw` wrapper is included.

### Configuration

All configuration is read from environment variables:

| Variable                 | Required | Default                                                                  | Description                                  |
|--------------------------|----------|--------------------------------------------------------------------------|----------------------------------------------|
| `JWT_SECRET`             | **yes**  | –                                                                        | HMAC key for signing tokens, at least 32 characters |
| `JWT_EXPIRATION`         | no       | `1h`                                                                     | Token lifetime (`30m`, `2h`, `1d`, ...)       |
| `DB_URL`                 | no       | `jdbc:mysql://localhost:3306/RestApiDB?createDatabaseIfNotExist=true`    | JDBC URL                                     |
| `DB_USERNAME`            | no       | `root`                                                                   | Database user                                |
| `DB_PASSWORD`            | no       | *(empty)*                                                                | Database password                            |
| `CLOUDINARY_CLOUD_NAME`  | no       | *(empty)*                                                                | Cloudinary credentials. If they're missing, photo uploads return `502`, and everything else still works |
| `CLOUDINARY_API_KEY`     | no       | *(empty)*                                                                |                                              |
| `CLOUDINARY_API_SECRET`  | no       | *(empty)*                                                                |                                              |
| `PORT`                   | no       | `8080`                                                                   | HTTP port                                    |

The application refuses to start if `JWT_SECRET` is missing or shorter than 32 bytes.

### Run locally

```bash
export JWT_SECRET="change-me-to-a-long-random-string-of-32+-chars"
export DB_USERNAME=root
export DB_PASSWORD=your-local-password
./mvnw spring-boot:run
```

On Windows PowerShell, use `$env:JWT_SECRET="..."` instead of `export`.

Hibernate creates or updates the tables on startup (`ddl-auto=update`). The roles `ROLE_USER` and `ROLE_ADMIN` are inserted automatically.

### Make a user an admin

There is intentionally no public endpoint for this. Grant the role in the database:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'you@example.com' AND r.name = 'ROLE_ADMIN';
```

---

## Running the tests

```bash
./mvnw clean test
```

The tests need **no MySQL server**. They run against an in-memory H2 database in MySQL compatibility mode, configured in `src/test/resources/application.properties`.

| Test class                       | Type                         | What it covers                                                                 |
|----------------------------------|------------------------------|--------------------------------------------------------------------------------|
| `PostServiceTest`                | unit (Mockito)               | author taken from the token, ownership checks, partial update, cascading delete |
| `PostLikeServiceTest`            | unit                         | like / unlike, duplicate like, notification                                     |
| `CommentServiceTest`             | unit                         | comment ownership (regression test for the old inverted check), post-author moderation |
| `FollowServiceTest`              | unit                         | follow / unfollow rules, self-follow, duplicates                                |
| `UserServiceTest`                | unit                         | profile privacy, profile update, password change rules                          |
| `AuthServiceTest`                | unit                         | registration: hashing, default role, duplicate email                            |
| `NotificationServiceTest`        | unit                         | no self-notifications, users can only read their own notifications             |
| `JwtServiceTest`                 | unit                         | valid, expired, tampered and malformed tokens, minimum secret length            |
| `SocialApiIntegrationTest`       | integration (MockMvc + H2)   | full HTTP flows through the real security filter chain                          |

Build the executable jar (this also runs the tests):

```bash
./mvnw clean package
java -jar target/Social_media_RestApi_Project-0.0.1-SNAPSHOT.jar
```

---

## API reference

Every endpoint except register and login requires this header:

```
Authorization: Bearer <token>
```

List endpoints accept `?page=0&size=10&sort=createdAt,desc`.

### Auth

| Method | Endpoint             | Description                        | Success |
|--------|----------------------|------------------------------------|---------|
| POST   | `/api/auth/register` | Create an account, returns a token | 201     |
| POST   | `/api/auth/login`    | Log in, returns a token            | 200     |

### Users & account

| Method | Endpoint                         | Description                                        | Success |
|--------|----------------------------------|----------------------------------------------------|---------|
| GET    | `/api/users?query=ali`           | List or search users by name (sort: `name`, `createdAt`) | 200     |
| GET    | `/api/users/me`                  | My profile (includes email)                        | 200     |
| PATCH  | `/api/users/me`                  | Update name / email / bio (partial)                | 200     |
| PUT    | `/api/users/me/password`         | Change password                                    | 204     |
| POST   | `/api/users/me/profile-photo`    | Upload profile photo (`multipart/form-data`, field `file`) | 200 |
| GET    | `/api/users/{userId}`            | Public profile                                     | 200     |
| GET    | `/api/users/{userId}/posts`      | Posts of a user                                    | 200     |
| POST   | `/api/users/{userId}/follow`     | Follow a user                                      | 204     |
| DELETE | `/api/users/{userId}/follow`     | Unfollow a user                                    | 204     |
| GET    | `/api/users/{userId}/followers`  | Followers                                          | 200     |
| GET    | `/api/users/{userId}/following`  | Users followed                                     | 200     |

### Posts & likes

| Method | Endpoint                     | Description                                       | Success |
|--------|------------------------------|---------------------------------------------------|---------|
| GET    | `/api/posts`                 | All posts (sort: `createdAt`, `updatedAt`, `title`) | 200   |
| GET    | `/api/posts/feed`            | My posts plus posts of users I follow             | 200     |
| GET    | `/api/posts/{postId}`        | A single post                                     | 200     |
| POST   | `/api/posts`                 | Create a post (returns a `Location` header)       | 201     |
| PATCH  | `/api/posts/{postId}`        | Edit a post (author or admin)                     | 200     |
| DELETE | `/api/posts/{postId}`        | Delete a post and its comments, likes and notifications (author or admin) | 204 |
| POST   | `/api/posts/{postId}/like`   | Like a post                                       | 204     |
| DELETE | `/api/posts/{postId}/like`   | Remove a like                                     | 204     |

### Comments

| Method | Endpoint                                   | Description                                    | Success |
|--------|--------------------------------------------|------------------------------------------------|---------|
| GET    | `/api/posts/{postId}/comments`             | Comments of a post                             | 200     |
| POST   | `/api/posts/{postId}/comments`             | Add a comment                                  | 201     |
| GET    | `/api/posts/{postId}/comments/{commentId}` | A single comment                               | 200     |
| PATCH  | `/api/posts/{postId}/comments/{commentId}` | Edit (comment author or admin)                 | 200     |
| DELETE | `/api/posts/{postId}/comments/{commentId}` | Delete (comment author, post author or admin)  | 204     |

### Notifications

| Method | Endpoint                                  | Description                         | Success |
|--------|-------------------------------------------|-------------------------------------|---------|
| GET    | `/api/notifications?unreadOnly=false`     | My notifications, newest first      | 200     |
| GET    | `/api/notifications/unread-count`         | Number of unread notifications      | 200     |
| PATCH  | `/api/notifications/{id}/read`            | Mark one as read                    | 204     |
| PATCH  | `/api/notifications/read-all`             | Mark all as read                    | 204     |

### Roles (admin only)

| Method | Endpoint      | Description                     | Success |
|--------|---------------|---------------------------------|---------|
| GET    | `/api/roles`  | List roles                      | 200     |
| POST   | `/api/roles`  | Create a role, e.g. `ROLE_MODERATOR` | 201 |

### Example requests

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Ali Kassab","email":"ali@example.com","password":"password123"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": 1, "name": "Ali Kassab", "profileImageUrl": null }
}
```

```bash
# Create a post
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Hello","content":"My first post"}'
```

```bash
# Read my feed, 5 posts per page
curl "http://localhost:8080/api/posts/feed?page=0&size=5" -H "Authorization: Bearer $TOKEN"
```

```json
{
  "content": [
    {
      "id": 7,
      "title": "Hello",
      "content": "My first post",
      "author": { "id": 1, "name": "Ali Kassab", "profileImageUrl": null },
      "likeCount": 3,
      "commentCount": 1,
      "likedByCurrentUser": false,
      "createdAt": "2026-01-10T12:00:00Z",
      "updatedAt": "2026-01-10T12:00:00Z"
    }
  ],
  "page": 0, "size": 5, "totalElements": 1, "totalPages": 1, "first": true, "last": true
}
```

### Error format

All errors use [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) problem details:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/auth/register",
  "errors": { "email": "Email must be a valid email address" }
}
```

| Status | When                                                                        |
|--------|-----------------------------------------------------------------------------|
| 400    | Validation errors, malformed JSON, business-rule violations, unsupported sort |
| 401    | Missing, invalid or expired token, or wrong login credentials               |
| 403    | Modifying someone else's resource, or a missing role                        |
| 404    | Resource not found                                                          |
| 409    | Duplicate email, duplicate like or follow, existing role                    |
| 413    | Uploaded file too large                                                     |
| 502    | Image storage unavailable or not configured                                 |

---

## Known limitations

- There is no refresh token and no server-side logout. A token stays valid until it expires.
- The schema is managed by Hibernate `ddl-auto=update`. A production setup would use versioned migrations (Flyway or Liquibase).
- There is no rate limiting on login.
- Notifications are polled through the REST API. There are no real-time push notifications.
- Accounts created before roles were assigned automatically may have no role. They can be fixed with:
  ```sql
  INSERT INTO user_roles (user_id, role_id)
  SELECT u.id, r.id FROM users u JOIN roles r ON r.name = 'ROLE_USER'
  WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id);
  ```

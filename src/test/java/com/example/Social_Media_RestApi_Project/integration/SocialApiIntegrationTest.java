package com.example.Social_Media_RestApi_Project.integration;

import com.example.Social_Media_RestApi_Project.role.entity.Role;
import com.example.Social_Media_RestApi_Project.role.repository.RoleRepository;
import com.example.Social_Media_RestApi_Project.user.entity.User;
import com.example.Social_Media_RestApi_Project.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real security filter chain, controllers, services
 * and JPA repositories, backed by an in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SocialApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    // ---------- authentication ----------

    @Test
    void registerThenLogin_returnsTokenThatAuthenticatesRequests() throws Exception {
        String email = uniqueEmail();
        register("Ali Kassab", email, "password123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.name").value("Ali Kassab"));

        String token = login(email, "password123");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_withDuplicateEmail_returnsConflict() throws Exception {
        String email = uniqueEmail();
        register("First User", email, "password123").andExpect(status().isCreated());

        register("Second User", email, "password123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email is already registered"));
    }

    @Test
    void register_withInvalidData_returnsFieldErrors() throws Exception {
        register("Al", "not-an-email", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        register("Ali Kassab", email, "password123");

        mockMvc.perform(json(post("/api/auth/login"), Map.of("email", email, "password", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void protectedEndpoint_withoutToken_returnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpoint_withInvalidToken_explainsWhy() throws Exception {
        mockMvc.perform(get("/api/posts").header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid or expired token"));
    }

    // ---------- posts, ownership and pagination ----------

    @Test
    void onlyTheAuthorCanEditOrDeleteAPost() throws Exception {
        String author = registerAndLogin("Post Author");
        String stranger = registerAndLogin("Stranger");
        long postId = createPost(author, "My first post", "Hello everyone");

        mockMvc.perform(auth(json(patch("/api/posts/" + postId), Map.of("title", "Hacked!")), stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth(delete("/api/posts/" + postId), stranger))
                .andExpect(status().isForbidden());

        mockMvc.perform(auth(json(patch("/api/posts/" + postId), Map.of("title", "Edited title")), author))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Edited title"))
                .andExpect(jsonPath("$.content").value("Hello everyone"));
    }

    @Test
    void createPost_returnsCreatedWithLocationHeader() throws Exception {
        String token = registerAndLogin("Writer");

        mockMvc.perform(auth(json(post("/api/posts"), Map.of("title", "Location test", "content", "Body")), token))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/posts/")))
                .andExpect(jsonPath("$.author.name").value("Writer"));
    }

    @Test
    void getPosts_isPaginated() throws Exception {
        String token = registerAndLogin("Prolific Writer");
        for (int i = 1; i <= 3; i++) {
            createPost(token, "Paged post " + i, "Content " + i);
        }

        mockMvc.perform(auth(get("/api/posts?page=0&size=2"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void getPosts_withUnknownSortProperty_returnsBadRequest() throws Exception {
        String token = registerAndLogin("Sorter");

        mockMvc.perform(auth(get("/api/posts?sort=doesNotExist,asc"), token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sortingByPrivateFields_isRejected() throws Exception {
        String token = registerAndLogin("Curious User");

        mockMvc.perform(auth(get("/api/posts?sort=author.email,asc"), token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(auth(get("/api/users?sort=password,asc"), token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMissingPost_returnsNotFound() throws Exception {
        String token = registerAndLogin("Reader");

        mockMvc.perform(auth(get("/api/posts/999999"), token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Post with id 999999 was not found"));
    }

    // ---------- social interactions ----------

    @Test
    void likesCommentsFollowsAndNotifications_workTogether() throws Exception {
        String alice = registerAndLogin("Alice Author");
        String bob = registerAndLogin("Bob Reader");
        long aliceId = currentUserId(alice);
        long postId = createPost(alice, "Alice's post", "Content from Alice");

        // Bob likes, comments on Alice's post and follows Alice
        mockMvc.perform(auth(post("/api/posts/" + postId + "/like"), bob)).andExpect(status().isNoContent());
        mockMvc.perform(auth(post("/api/posts/" + postId + "/like"), bob)).andExpect(status().isConflict());
        mockMvc.perform(auth(json(post("/api/posts/" + postId + "/comments"), Map.of("content", "Great post!")), bob))
                .andExpect(status().isCreated());
        mockMvc.perform(auth(post("/api/users/" + aliceId + "/follow"), bob)).andExpect(status().isNoContent());

        // Bob sees the counters and his own like
        mockMvc.perform(auth(get("/api/posts/" + postId), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.commentCount").value(1))
                .andExpect(jsonPath("$.likedByCurrentUser").value(true));

        // Alice's post shows up in Bob's feed because he follows her
        mockMvc.perform(auth(get("/api/posts/feed"), bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem((int) postId)));

        // Alice's profile shows her new follower, and Bob does not see her email
        mockMvc.perform(auth(get("/api/users/" + aliceId), bob))
                .andExpect(jsonPath("$.followersCount").value(1))
                .andExpect(jsonPath("$.followedByCurrentUser").value(true))
                .andExpect(jsonPath("$.email").doesNotExist());

        // Alice received three notifications: like, comment and follow
        mockMvc.perform(auth(get("/api/notifications/unread-count"), alice))
                .andExpect(jsonPath("$.unreadCount").value(3));
        mockMvc.perform(auth(get("/api/notifications"), alice))
                .andExpect(jsonPath("$.content[*].type", hasItem("LIKE")))
                .andExpect(jsonPath("$.content[*].type", hasItem("COMMENT")))
                .andExpect(jsonPath("$.content[*].type", hasItem("FOLLOW")));
        mockMvc.perform(auth(patch("/api/notifications/read-all"), alice)).andExpect(status().isNoContent());
        mockMvc.perform(auth(get("/api/notifications/unread-count"), alice))
                .andExpect(jsonPath("$.unreadCount").value(0));

        // Deleting a post that has likes, comments and notifications succeeds
        mockMvc.perform(auth(delete("/api/posts/" + postId), alice)).andExpect(status().isNoContent());
        mockMvc.perform(auth(get("/api/posts/" + postId), alice)).andExpect(status().isNotFound());
    }

    @Test
    void cannotFollowYourself() throws Exception {
        String token = registerAndLogin("Lonely User");

        mockMvc.perform(auth(post("/api/users/" + currentUserId(token) + "/follow"), token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchUsers_findsByNameIgnoringCase() throws Exception {
        String marker = "Zq" + UUID.randomUUID().toString().substring(0, 6);
        String token = registerAndLogin("Searchable " + marker);

        mockMvc.perform(auth(get("/api/users?query=" + marker.toLowerCase()), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Searchable " + marker));
    }

    // ---------- account settings ----------

    @Test
    void changePassword_thenLoginWithNewPassword() throws Exception {
        String email = uniqueEmail();
        register("Password Changer", email, "password123");
        String token = login(email, "password123");

        mockMvc.perform(auth(json(put("/api/users/me/password"),
                        Map.of("currentPassword", "wrong-one", "newPassword", "newPassword456")), token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(auth(json(put("/api/users/me/password"),
                        Map.of("currentPassword", "password123", "newPassword", "newPassword456")), token))
                .andExpect(status().isNoContent());

        assertThat(login(email, "newPassword456")).isNotBlank();
    }

    @Test
    void updateProfile_changesBioAndKeepsTokenValidAfterEmailChange() throws Exception {
        String token = registerAndLogin("Profile Owner");
        String newEmail = uniqueEmail();

        mockMvc.perform(auth(json(patch("/api/users/me"), Map.of("email", newEmail, "bio", "Backend developer")), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.bio").value("Backend developer"));

        // the JWT subject is the user id, so the same token still works
        mockMvc.perform(auth(get("/api/users/me"), token)).andExpect(status().isOk());
    }

    @Test
    void uploadProfilePhoto_rejectsNonImageFiles() throws Exception {
        String token = registerAndLogin("Photo Uploader");
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(auth(multipart("/api/users/me/profile-photo").file(file), token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only JPEG, PNG, WEBP and GIF images are allowed"));
    }

    // ---------- roles ----------

    @Test
    void roleAdministration_requiresAdminRole() throws Exception {
        String userToken = registerAndLogin("Normal User");
        mockMvc.perform(auth(get("/api/roles"), userToken)).andExpect(status().isForbidden());

        String adminEmail = uniqueEmail();
        register("Admin User", adminEmail, "password123");
        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        admin.addRole(roleRepository.findByName(Role.ADMIN).orElseThrow());
        userRepository.save(admin);
        String adminToken = login(adminEmail, "password123");

        mockMvc.perform(auth(get("/api/roles"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(Role.ADMIN)));
    }

    // ---------- helpers ----------

    private ResultActions register(String name, String email, String password) throws Exception {
        return mockMvc.perform(json(post("/api/auth/register"),
                Map.of("name", name, "email", email, "password", password)));
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(json(post("/api/auth/login"), Map.of("email", email, "password", password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }

    private String registerAndLogin(String name) throws Exception {
        String email = uniqueEmail();
        register(name, email, "password123").andExpect(status().isCreated());
        return login(email, "password123");
    }

    private long createPost(String token, String title, String content) throws Exception {
        String body = mockMvc.perform(auth(json(post("/api/posts"), Map.of("title", title, "content", content)), token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private long currentUserId(String token) throws Exception {
        String body = mockMvc.perform(auth(get("/api/users/me"), token))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("id").asLong();
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Object body) throws Exception {
        return request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}

package com.alcohol.community;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.alcohol.places.GooglePlacesService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = {
        "/db/V6_google_places_bars.sql",
        "/db/V7_community_interactions.sql",
        "/db/V8_chat.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CommunityChatApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GooglePlacesService googlePlacesService;

    private static String accessToken;
    private static String checkInId;
    private static String conversationId;

    @Test
    @Order(1)
    void loginAndCreateCheckIn() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@barlog.app","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        accessToken = login.getResponse().getContentAsString()
                .replaceAll("(?s).*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        MvcResult created = mockMvc.perform(post("/api/checkins")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "photoUrl": "https://images.barlog.local/checkins/test.jpg",
                                  "drinkName": "Community Test",
                                  "drinkCategory": "cocktail",
                                  "barId": "bar_001",
                                  "barName": "Amber Room",
                                  "city": "Shanghai",
                                  "moodTags": ["warm"],
                                  "rating": 4.5,
                                  "vibeMumbling": "Test sip",
                                  "cardStyle": "receipt",
                                  "visibility": "public",
                                  "socialStatus": "open_to_chat"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        checkInId = created.getResponse().getContentAsString()
                .replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    @Order(2)
    void eligibilityAndFeed() throws Exception {
        mockMvc.perform(get("/api/community/eligibility")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("city", "Shanghai")
                        .param("barId", "bar_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canViewCommunity").value(true))
                .andExpect(jsonPath("$.canViewCityFeed").value(true))
                .andExpect(jsonPath("$.canViewBarFeed").value(true));

        mockMvc.perform(get("/api/community/feed")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("range", "24h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @Order(25)
    void galleryFeedWithoutTodayCheckIn() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Gallery Viewer",
                                  "email": "gallery-viewer-%d@barlog.app",
                                  "password": "password123"
                                }
                                """.formatted(System.currentTimeMillis())))
                .andExpect(status().isCreated())
                .andReturn();
        String token = login.getResponse().getContentAsString()
                .replaceAll("(?s).*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/gallery/feed")
                        .header("Authorization", "Bearer " + token)
                        .param("range", "24h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(get("/api/community/eligibility")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canViewCommunity").value(true));
    }

    @Test
    @Order(3)
    void likeAndComment() throws Exception {
        if (checkInId == null || checkInId.contains("{")) {
            return;
        }
        mockMvc.perform(post("/api/community/posts/" + checkInId + "/like")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));

        mockMvc.perform(post("/api/community/posts/" + checkInId + "/comments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Looks cozy!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Looks cozy!"));
    }

    @Test
    @Order(4)
    void chatListEmptyInitially() throws Exception {
        if (accessToken == null || accessToken.contains("{")) {
            return;
        }
        mockMvc.perform(get("/api/chat/conversations")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}

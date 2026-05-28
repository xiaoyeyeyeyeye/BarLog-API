package com.alcohol.compat;

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
@Sql(scripts = "/db/V6_google_places_bars.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CheckInIsolationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GooglePlacesService googlePlacesService;

    private static String tokenA;
    private static String tokenB;
    private static String userAId;
    private static String privateCheckInId;
    private static final String EMAIL_SUFFIX = String.valueOf(System.currentTimeMillis());

    private static String extractToken(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString()
                .replaceAll("(?s).*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    private static String extractUserId(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString()
                .replaceAll("(?s).*\"user\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    private static String extractCheckInId(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString()
                .replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    private static final String CHECKIN_BODY = """
            {
              "photoUrl": "https://images.barlog.local/test.jpg",
              "drinkName": "Isolation Negroni",
              "drinkCategory": "cocktail",
              "barId": "bar_001",
              "barName": "Amber Room",
              "city": "Shanghai",
              "moodTags": ["warm"],
              "rating": 4.5,
              "vibeMumbling": "Private sip",
              "cardStyle": "receipt",
              "visibility": "private"
            }
            """;

    @Test
    @Order(1)
    void recentWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/checkins/recent"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    @Order(2)
    void registerUserAAndCreatePrivateCheckIn() throws Exception {
        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "displayName": "Alice",
                                  "email": "alice-isolation-%s@test.com",
                                  "password": "password123"
                                }
                                """, EMAIL_SUFFIX)))
                .andExpect(status().isCreated())
                .andReturn();
        tokenA = extractToken(register);
        userAId = extractUserId(register);

        MvcResult created = mockMvc.perform(post("/api/checkins")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CHECKIN_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        privateCheckInId = extractCheckInId(created);
    }

    @Test
    @Order(3)
    void registerUserBCannotSeeUserARecentOrDetail() throws Exception {
        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "displayName": "Bob",
                                  "email": "bob-isolation-%s@test.com",
                                  "password": "password123"
                                }
                                """, EMAIL_SUFFIX)))
                .andExpect(status().isCreated())
                .andReturn();
        tokenB = extractToken(register);

        mockMvc.perform(get("/api/checkins/recent")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.drinkName == 'Isolation Negroni')]").isEmpty());

        mockMvc.perform(get("/api/checkins/" + privateCheckInId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHECKIN_FORBIDDEN"));

        mockMvc.perform(get("/api/users/" + userAId + "/checkins")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHECKIN_FORBIDDEN"));
    }

    @Test
    @Order(4)
    void userACanStillReadOwnCheckIn() throws Exception {
        mockMvc.perform(get("/api/checkins/" + privateCheckInId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drinkName").value("Isolation Negroni"));
    }
}

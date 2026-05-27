package com.alcohol.compat;

import org.junit.jupiter.api.BeforeEach;
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

import com.alcohol.compat.vo.FrontendBarVO;
import com.alcohol.places.GooglePlacesService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 前端契约 API 集成测试（需本地 PostgreSQL alcohol 库已初始化）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = "/db/V6_google_places_bars.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class FrontendCompatApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GooglePlacesService googlePlacesService;

    private static String accessToken;
    private static String checkinId;

    @BeforeEach
    void disableGooglePlacesByDefault() {
        when(googlePlacesService.isAvailable()).thenReturn(false);
    }

    @Test
    @Order(1)
    void health() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    @Order(2)
    void anonymousBootstrapDemoUser() throws Exception {
        mockMvc.perform(get("/api/checkins/recent"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    void loginDemoUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@barlog.app","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("demo@barlog.app"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        accessToken = body.replaceAll("(?s).*\"accessToken\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    @Order(4)
    void diarySummaryWithoutTokenUsesAnonymous() throws Exception {
        mockMvc.perform(get("/api/diary/summary").param("month", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-05"))
                .andExpect(jsonPath("$.checkInCount").isNumber());
    }

    @Test
    @Order(5)
    void recentCheckinsReturnsItemsArray() throws Exception {
        mockMvc.perform(get("/api/checkins/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @Order(6)
    void nearbyBarsShanghai() throws Exception {
        mockMvc.perform(get("/api/bars/nearby").param("city", "Shanghai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].city").value("Shanghai"));
    }

    @Test
    @Order(7)
    void createCheckIn() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/checkins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "photoUrl": "https://images.barlog.local/test.jpg",
                                  "drinkName": "Smoked Negroni",
                                  "drinkCategory": "cocktail",
                                  "barId": "bar_001",
                                  "barName": "Amber Room",
                                  "city": "Shanghai",
                                  "moodTags": ["warm","bitter"],
                                  "rating": 4.5,
                                  "vibeMumbling": "Test sip",
                                  "cardStyle": "receipt",
                                  "visibility": "private"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.drinkCategory").value("cocktail"))
                .andReturn();
        checkinId = result.getResponse().getContentAsString()
                .replaceAll("(?s).*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }

    @Test
    @Order(8)
    void getCheckInDetail() throws Exception {
        if (checkinId == null || checkinId.contains("{")) {
            return;
        }
        mockMvc.perform(get("/api/checkins/" + checkinId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drinkName").value("Smoked Negroni"));
    }

    @Test
    @Order(9)
    void galleryFeed() throws Exception {
        mockMvc.perform(get("/api/gallery/feed").param("city", "Shanghai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @Order(10)
    void aiGenerateCardCopy() throws Exception {
        mockMvc.perform(post("/api/ai/generate-card-copy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"drinkName\":\"Martini\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.style").value("receipt"));
    }

    @Test
    @Order(11)
    void recentCheckinsMoodTagsIsArray() throws Exception {
        mockMvc.perform(get("/api/checkins/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].moodTags").isArray());
    }

    @Test
    @Order(13)
    void nearbyBarsUsesGoogleWhenEnabled() throws Exception {
        FrontendBarVO bar = new FrontendBarVO();
        bar.setId("gp_test");
        bar.setName("Google Bar");
        bar.setCity("Singapore");
        bar.setRating(4.5);
        bar.setCheckInCount(0);
        when(googlePlacesService.isAvailable()).thenReturn(true);
        when(googlePlacesService.searchNearby(isNull(), isNull(), eq("Singapore")))
                .thenReturn(List.of(bar));

        mockMvc.perform(get("/api/bars/nearby").param("city", "Singapore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("gp_test"))
                .andExpect(jsonPath("$[0].name").value("Google Bar"));
    }

    @Test
    @Order(14)
    void barDetailSeedBar() throws Exception {
        mockMvc.perform(get("/api/bars/bar_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("bar_001"))
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.checkInCount").isNumber());
    }

    @Test
    @Order(12)
    void authMeWithToken() throws Exception {
        if (accessToken == null || accessToken.contains("{")) {
            return;
        }
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("demo@barlog.app"));
    }
}

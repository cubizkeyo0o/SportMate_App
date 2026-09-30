package com.sportmate.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.sportmate.app.data.remote.api.UserApi;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.testutil.BlockingCallback;
import com.sportmate.app.testutil.FakeTokenStorage;
import com.sportmate.app.testutil.TestNetwork;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** api/v1/users/me/sports against the backend's JSON shapes. */
public class UserSportProfileRepositoryImplTest {
    private static final String TENNIS_ID = "3b0c8f2e-1d2a-4f3b-9c4d-5e6f7a8b9c01";

    private MockWebServer server;
    private UserSportProfileRepositoryImpl repository;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        UserApi api = TestNetwork.retrofit(server, new FakeTokenStorage("access-1", "refresh-1")).create(UserApi.class);
        repository = new UserSportProfileRepositoryImpl(api, TestNetwork.executor());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void listMapsSportSummaryAndSkillLevel() throws Exception {
        server.enqueue(new MockResponse().setBody("["
                + profile("a1", "Badminton", "badminton", true, "expert") + ","
                + profile(TENNIS_ID, "Tennis", "tennis", false, "beginner") + "]"));

        BlockingCallback<List<UserSportProfile>> callback = new BlockingCallback<>();
        repository.getSportProfiles(callback);
        List<UserSportProfile> profiles = callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/users/me/sports", request.getPath());
        assertEquals(2, profiles.size());
        assertEquals("Badminton", profiles.get(0).getSport().getName());
        assertEquals("badminton", profiles.get(0).getSport().getSlug());
        assertTrue(profiles.get(0).isSportActive());
        assertEquals(SkillLevel.EXPERT, profiles.get(0).getSkillLevel());
        assertEquals(TENNIS_ID, profiles.get(1).getSport().getId());
        assertFalse(profiles.get(1).isSportActive());
        assertEquals(SkillLevel.BEGINNER, profiles.get(1).getSkillLevel());
    }

    @Test
    public void emptyListIsSuccess() throws Exception {
        server.enqueue(new MockResponse().setBody("[]"));

        BlockingCallback<List<UserSportProfile>> callback = new BlockingCallback<>();
        repository.getSportProfiles(callback);

        assertTrue(callback.await().result().isEmpty());
    }

    @Test
    public void unknownSkillLevelIsAnUnreadableResponse() throws Exception {
        server.enqueue(new MockResponse().setBody("[" + profile(TENNIS_ID, "Tennis", "tennis", true, "grandmaster") + "]"));

        BlockingCallback<List<UserSportProfile>> callback = new BlockingCallback<>();
        repository.getSportProfiles(callback);

        assertEquals(AppError.Kind.UNKNOWN, callback.await().error().getKind());
    }

    @Test
    public void setSendsSkillLevelNameToSportPath() throws Exception {
        server.enqueue(new MockResponse().setBody(profile(TENNIS_ID, "Tennis", "tennis", true, "intermediate")));

        UserSportProfile saved = set(SkillLevel.INTERMEDIATE).result();

        RecordedRequest request = server.takeRequest();
        assertEquals("PUT", request.getMethod());
        assertEquals("/api/v1/users/me/sports/" + TENNIS_ID, request.getPath());
        assertEquals("{\"skill_level\":\"intermediate\"}", request.getBody().readUtf8());
        assertEquals(SkillLevel.INTERMEDIATE, saved.getSkillLevel());
        assertEquals("Tennis", saved.getSport().getName());
    }

    @Test
    public void everySkillLevelUsesTheBackendName() throws Exception {
        String[] expected = {"beginner", "elementary", "intermediate", "advanced", "expert"};
        SkillLevel[] levels = SkillLevel.values();
        assertEquals(expected.length, levels.length);
        for (int i = 0; i < levels.length; i++) {
            server.enqueue(new MockResponse().setBody(profile(TENNIS_ID, "Tennis", "tennis", true, expected[i])));

            assertEquals(levels[i], set(levels[i]).result().getSkillLevel());
            assertEquals("{\"skill_level\":\"" + expected[i] + "\"}", server.takeRequest().getBody().readUtf8());
        }
    }

    @Test
    public void setForInactiveSportReturnsSportIdValidationError() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(400).setBody(
                "{\"title\":\"One or more validation errors occurred.\",\"status\":400,"
                        + "\"errors\":{\"sport_id\":[\"This sport is no longer available for new profiles.\"]}}"));

        AppError error = set(SkillLevel.BEGINNER).error();

        assertEquals(AppError.Kind.VALIDATION, error.getKind());
        assertEquals(Collections.singletonList("This sport is no longer available for new profiles."),
                error.getFieldErrors().get("sport_id"));
    }

    @Test
    public void setForUnknownSportIsNotFoundAndRaceIsConflict() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"title\":\"Sport not found.\",\"status\":404}"));
        server.enqueue(new MockResponse().setResponseCode(409)
                .setBody("{\"title\":\"The sport profile was changed by another request. Please retry.\",\"status\":409}"));

        AppError notFound = set(SkillLevel.BEGINNER).error();
        AppError conflict = set(SkillLevel.BEGINNER).error();

        assertEquals(AppError.Kind.NOT_FOUND, notFound.getKind());
        assertEquals("Sport not found.", notFound.getMessage());
        assertEquals(AppError.Kind.CONFLICT, conflict.getKind());
    }

    @Test
    public void removeSendsDeleteToSportPath() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(204));

        BlockingCallback<Void> callback = new BlockingCallback<>();
        repository.removeSportProfile(TENNIS_ID, callback);
        callback.await().result();

        RecordedRequest request = server.takeRequest();
        assertEquals("DELETE", request.getMethod());
        assertEquals("/api/v1/users/me/sports/" + TENNIS_ID, request.getPath());
    }

    @Test
    public void removeFailuresAreReported() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(401));
        assertEquals(AppError.Kind.UNAUTHORIZED, remove().error().getKind());

        server.shutdown();
        assertEquals(AppError.Kind.NETWORK, remove().error().getKind());
    }

    private BlockingCallback<UserSportProfile> set(SkillLevel level) throws InterruptedException {
        BlockingCallback<UserSportProfile> callback = new BlockingCallback<>();
        repository.setSportProfile(TENNIS_ID, level, callback);
        return callback.await();
    }

    private BlockingCallback<Void> remove() throws InterruptedException {
        BlockingCallback<Void> callback = new BlockingCallback<>();
        repository.removeSportProfile(TENNIS_ID, callback);
        return callback.await();
    }

    private static String profile(String id, String name, String slug, boolean active, String skillLevel) {
        return "{\"sport\":{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"slug\":\"" + slug + "\","
                + "\"is_active\":" + active + "},\"skill_level\":\"" + skillLevel + "\"}";
    }
}

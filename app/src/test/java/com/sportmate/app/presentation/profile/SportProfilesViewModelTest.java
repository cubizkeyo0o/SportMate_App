package com.sportmate.app.presentation.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.R;
import com.sportmate.app.domain.common.AppError;
import com.sportmate.app.domain.model.SkillLevel;
import com.sportmate.app.domain.model.Sport;
import com.sportmate.app.domain.model.UserSportProfile;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.presentation.common.UiState;
import com.sportmate.app.presentation.common.UiText;
import com.sportmate.app.testutil.ControllableProfileRepositories;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SportProfilesViewModelTest {
    private static final Sport BADMINTON = ControllableProfileRepositories.sport("s-badminton", "Badminton");
    private static final Sport PICKLEBALL = ControllableProfileRepositories.sport("s-pickleball", "Pickleball");
    private static final Sport TENNIS = ControllableProfileRepositories.sport("s-tennis", "Tennis");

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private final ControllableProfileRepositories.SportProfiles profilesRepo = new ControllableProfileRepositories.SportProfiles();
    private final ControllableProfileRepositories.Sports sportsRepo = new ControllableProfileRepositories.Sports();
    private SportProfilesViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new SportProfilesViewModel(profilesRepo, sportsRepo);
    }

    @Test
    public void loadingUntilBothProfilesAndCatalogArrive() {
        assertEquals(UiState.Status.LOADING, status());

        profilesRepo.listCallback.onSuccess(Collections.singletonList(profile(TENNIS, SkillLevel.ADVANCED)));
        assertEquals(UiState.Status.LOADING, status());

        sportsRepo.callback.onSuccess(Arrays.asList(BADMINTON, PICKLEBALL, TENNIS));
        assertEquals(UiState.Status.SUCCESS, status());
        assertEquals(1, profiles().size());
        assertEquals(Arrays.asList(BADMINTON, PICKLEBALL), viewModel.getAddableSports().getValue());
    }

    @Test
    public void emptyProfileListIsSuccessWithNoItems() {
        loaded(Collections.emptyList(), Collections.singletonList(TENNIS));

        assertEquals(UiState.Status.SUCCESS, status());
        assertTrue(profiles().isEmpty());
        assertEquals(Collections.singletonList(TENNIS), viewModel.getAddableSports().getValue());
    }

    @Test
    public void failureOfEitherRequestIsErrorAndRetryReloadsBoth() {
        AppError error = AppError.of(AppError.Kind.NETWORK);
        sportsRepo.callback.onError(error);

        assertEquals(UiState.Status.ERROR, status());
        assertSame(error, viewModel.getProfiles().getValue().getError());
        assertEquals(1, profilesRepo.cancelled);

        // A late answer to the abandoned request must not override the error.
        profilesRepo.listCallback.onSuccess(Collections.emptyList());
        assertEquals(UiState.Status.ERROR, status());

        viewModel.retry();
        assertEquals(UiState.Status.LOADING, status());
        assertEquals(2, profilesRepo.listCalls);
        assertEquals(2, sportsRepo.calls);

        profilesRepo.listCallback.onSuccess(Collections.emptyList());
        sportsRepo.callback.onSuccess(Collections.singletonList(TENNIS));
        assertEquals(UiState.Status.SUCCESS, status());
    }

    @Test
    public void addingASportInsertsItInNameOrderAndRemovesItFromAddable() {
        loaded(Collections.singletonList(profile(TENNIS, SkillLevel.ADVANCED)), Arrays.asList(BADMINTON, TENNIS));

        viewModel.setSkillLevel(BADMINTON.getId(), SkillLevel.BEGINNER);
        assertTrue(viewModel.getUpdating().getValue());
        assertEquals(BADMINTON.getId(), profilesRepo.lastSportId);
        assertEquals(SkillLevel.BEGINNER, profilesRepo.lastSkillLevel);

        profilesRepo.setCallback.onSuccess(profile(BADMINTON, SkillLevel.BEGINNER));

        assertFalse(viewModel.getUpdating().getValue());
        assertEquals("Badminton", profiles().get(0).getSport().getName());
        assertEquals("Tennis", profiles().get(1).getSport().getName());
        assertTrue(viewModel.getAddableSports().getValue().isEmpty());
        assertMessage(UiText.of(R.string.sport_profile_saved));
    }

    @Test
    public void updatingASkillLevelReplacesTheProfile() {
        loaded(Collections.singletonList(profile(TENNIS, SkillLevel.BEGINNER)), Collections.singletonList(TENNIS));

        viewModel.setSkillLevel(TENNIS.getId(), SkillLevel.EXPERT);
        profilesRepo.setCallback.onSuccess(profile(TENNIS, SkillLevel.EXPERT));

        assertEquals(1, profiles().size());
        assertEquals(SkillLevel.EXPERT, profiles().get(0).getSkillLevel());
    }

    @Test
    public void removingAProfileMakesItsSportAddableAgain() {
        loaded(Collections.singletonList(profile(TENNIS, SkillLevel.BEGINNER)), Collections.singletonList(TENNIS));

        viewModel.remove(TENNIS.getId());
        profilesRepo.removeCallback.onSuccess(null);

        assertTrue(profiles().isEmpty());
        assertEquals(Collections.singletonList(TENNIS), viewModel.getAddableSports().getValue());
        assertMessage(UiText.of(R.string.sport_profile_removed));
    }

    @Test
    public void validationErrorShowsTheFieldMessageAndKeepsTheList() {
        loaded(Collections.emptyList(), Collections.singletonList(TENNIS));

        viewModel.setSkillLevel(TENNIS.getId(), SkillLevel.BEGINNER);
        profilesRepo.setCallback.onError(new AppError(AppError.Kind.VALIDATION, 400, "One or more validation errors occurred.",
                Collections.singletonMap("sport_id", Collections.singletonList("This sport is no longer available for new profiles."))));

        assertFalse(viewModel.getUpdating().getValue());
        assertTrue(profiles().isEmpty());
        assertMessage(UiText.raw("This sport is no longer available for new profiles."));
    }

    @Test
    public void otherChangeErrorsUseTheCommonErrorText() {
        loaded(Collections.singletonList(profile(TENNIS, SkillLevel.BEGINNER)), Collections.singletonList(TENNIS));

        viewModel.remove(TENNIS.getId());
        profilesRepo.removeCallback.onError(new AppError(AppError.Kind.SERVER, 500, null, null));

        assertEquals(1, profiles().size());
        assertMessage(UiText.of(R.string.error_server));
    }

    @Test
    public void changesAreIgnoredWhileUpdatingOrNotLoaded() {
        viewModel.setSkillLevel(TENNIS.getId(), SkillLevel.BEGINNER);
        assertEquals(0, profilesRepo.setCalls);

        loaded(Collections.emptyList(), Collections.singletonList(TENNIS));
        viewModel.setSkillLevel(TENNIS.getId(), SkillLevel.BEGINNER);
        viewModel.remove(TENNIS.getId());

        assertEquals(1, profilesRepo.setCalls);
        assertEquals(0, profilesRepo.removeCalls);
    }

    private void loaded(List<UserSportProfile> profiles, List<Sport> catalog) {
        profilesRepo.listCallback.onSuccess(profiles);
        sportsRepo.callback.onSuccess(catalog);
    }

    private UiState.Status status() {
        return viewModel.getProfiles().getValue().getStatus();
    }

    private List<UserSportProfile> profiles() {
        return viewModel.getProfiles().getValue().getData();
    }

    private void assertMessage(UiText expected) {
        Event<UiText> event = viewModel.getMessage().getValue();
        assertEquals(expected, event.getContentIfNotHandled());
        assertNull(event.getContentIfNotHandled());
    }

    private static UserSportProfile profile(Sport sport, SkillLevel level) {
        return new UserSportProfile(sport, true, level);
    }
}

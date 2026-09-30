package com.sportmate.app.presentation.splash;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.sportmate.app.data.repository.AuthRepositoryImpl;
import com.sportmate.app.presentation.common.Event;
import com.sportmate.app.testutil.FakeTokenStorage;

import org.junit.Rule;
import org.junit.Test;

public class SplashViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void routesToAuthWithoutStoredSession() {
        SplashViewModel viewModel = new SplashViewModel(repositoryWith(new FakeTokenStorage()));

        assertEquals(SplashViewModel.Destination.AUTH, viewModel.getDestination().getValue().getContentIfNotHandled());
    }

    @Test
    public void routesToMainWithStoredSessionOnlyOnce() {
        SplashViewModel viewModel = new SplashViewModel(repositoryWith(new FakeTokenStorage("access", "refresh")));

        Event<SplashViewModel.Destination> event = viewModel.getDestination().getValue();
        assertEquals(SplashViewModel.Destination.MAIN, event.getContentIfNotHandled());
        assertNull(event.getContentIfNotHandled());
    }

    // hasSession() only reads token storage, so no API or executor is needed.
    private static AuthRepositoryImpl repositoryWith(FakeTokenStorage tokenStorage) {
        return new AuthRepositoryImpl(null, tokenStorage, null);
    }
}

package com.sportmate.app.presentation;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import com.sportmate.app.R;
import com.sportmate.app.presentation.session.AuthState;
import com.sportmate.app.presentation.session.SessionViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Single activity hosting the navigation graph. It is the one place that moves between the auth
 * and main flows, driven by the session's {@link AuthState}; both moves clear the back stack.
 */
@AndroidEntryPoint
public final class MainActivity extends AppCompatActivity {
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // targetSdk 35+ draws edge-to-edge on Android 15+; opt in everywhere and pad for system bars
        // and the keyboard, so forms stay visible while typing.
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host), (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        navController = navHost.getNavController();

        SessionViewModel sessionViewModel = new ViewModelProvider(this).get(SessionViewModel.class);
        sessionViewModel.getAuthState().observe(this, this::onAuthStateChanged);
    }

    // Loading and error keep the current screen: Splash shows progress or a retry.
    private void onAuthStateChanged(AuthState state) {
        int flow = currentFlow();
        if (state.getStatus() == AuthState.Status.AUTHENTICATED && flow != R.id.main_nav_graph) {
            navController.navigate(R.id.action_global_main);
        } else if (state.getStatus() == AuthState.Status.UNAUTHENTICATED && flow != R.id.auth_nav_graph) {
            navController.navigate(R.id.action_global_auth);
        }
    }

    /** The nested graph (auth or main) holding the current destination; the root graph for Splash. */
    private int currentFlow() {
        NavDestination destination = navController.getCurrentDestination();
        NavGraph parent = destination == null ? null : destination.getParent();
        return parent == null ? 0 : parent.getId();
    }
}

package com.transitph.app.activities;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.transitph.app.R;
import com.transitph.app.fragments.AssistedNavigationFragment;
import com.transitph.app.fragments.HomeFragment;
import com.transitph.app.supabase.SupabaseSync;
import com.transitph.app.fragments.ProfileFragment;
import com.transitph.app.fragments.RouteFinderFragment;
import com.transitph.app.fragments.SavedRoutesFragment;
import com.transitph.app.fragments.TerminalsFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        setupBottomNav();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "HomeFragment");
            syncFromSupabase();
        }
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = createFragmentFor(itemId);
            if (selectedFragment != null) {
                loadFragment(selectedFragment, tagFor(itemId));
                return true;
            }
            return false;
        });
    }

    private Fragment createFragmentFor(int itemId) {
        if (itemId == R.id.nav_home) return new HomeFragment();
        if (itemId == R.id.nav_routes) return new RouteFinderFragment();
        if (itemId == R.id.nav_terminals) return new TerminalsFragment();
        if (itemId == R.id.nav_saved) return new SavedRoutesFragment();
        if (itemId == R.id.nav_profile) return new ProfileFragment();
        return null;
    }

    private String tagFor(int itemId) {
        if (itemId == R.id.nav_home) return "HomeFragment";
        if (itemId == R.id.nav_routes) return "RouteFinderFragment";
        if (itemId == R.id.nav_terminals) return "TerminalsFragment";
        if (itemId == R.id.nav_saved) return "SavedRoutesFragment";
        if (itemId == R.id.nav_profile) return "ProfileFragment";
        return "";
    }

    /** Pulls terminals/routes/saved routes from Supabase in the background, then reloads the visible tab. */
    private void syncFromSupabase() {
        final Context appContext = getApplicationContext();
        new Thread(() -> {
            boolean updated = SupabaseSync.pullAll(appContext);
            if (!updated) return;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || getSupportFragmentManager().isStateSaved()) return;
                int selected = bottomNav.getSelectedItemId();
                Fragment fresh = createFragmentFor(selected);
                if (fresh != null) loadFragment(fresh, tagFor(selected));
            });
        }).start();
    }

    public void loadFragment(Fragment fragment, String tag) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragment_container, fragment, tag);
        ft.commit();
    }

    public void switchToTab(int navItemId) {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(navItemId);
        }
    }

    public void openAssistedNavigation() {
        loadFragment(new AssistedNavigationFragment(), "AssistedNavigationFragment");
    }
}

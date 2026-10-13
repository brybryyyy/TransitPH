package com.transitph.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.transitph.app.R;
import com.transitph.app.activities.MainActivity;
import com.transitph.app.activities.RouteDetailsActivity;
import com.transitph.app.activities.RouteResultsActivity;
import com.transitph.app.adapters.SavedRouteAdapter;
import com.transitph.app.database.SavedRouteDao;
import com.transitph.app.models.SavedRoute;
import com.transitph.app.models.Terminal;
import com.transitph.app.utils.SessionManager;

import org.osmdroid.config.Configuration;
import com.transitph.app.map.TerminalMarkers;
import com.transitph.app.map.TomTomTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.List;

public class HomeFragment extends Fragment implements SavedRouteAdapter.OnSavedRouteClickListener {

    private EditText etFrom;
    private EditText etTo;
    private Button btnFindRoute;

    private org.osmdroid.views.MapView map;
    private com.transitph.app.database.TerminalDao terminalDao;

    // Quick Access
    private View btnQuickRoute;
    private View btnQuickTerminals;
    private View btnQuickAssisted;
    private View btnQuickSaved;
    private View btnQuickDestinations;
    private View btnQuickSafety;
    private View cardSearchLimit;

    private Button btnOpenAssistant;
    private TextView tvWelcomeUser;

    // Saved Routes Section
    private RecyclerView rvHomeSavedRoutes;
    private View layoutHomeNoSaved;
    private SavedRouteDao savedRouteDao;
    private SessionManager sessionManager;
    private SavedRouteAdapter savedRouteAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionManager = new SessionManager(requireContext());
        savedRouteDao = new SavedRouteDao(requireContext());
        terminalDao = new com.transitph.app.database.TerminalDao(requireContext());

        initViews(view);
        setupListeners();
        loadSavedRoutesPreview();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) {
            map.onResume();
        }
        loadSavedRoutesPreview();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) {
            map.onPause();
        }
    }

    private void initViews(View view) {
        etFrom = view.findViewById(R.id.et_home_from);
        etTo = view.findViewById(R.id.et_home_to);
        btnFindRoute = view.findViewById(R.id.btn_home_find_route);

        map = view.findViewById(R.id.map_home);
        if (map != null) {
            TomTomTileSource.apply(map);   // TomTom tiles
            map.setMultiTouchControls(true);
            GeoPoint startPoint = new GeoPoint(14.2132, 121.1648);
            map.getController().setZoom(10.0);
            map.getController().setCenter(startPoint);
            plotTerminals();
        }

        btnQuickRoute = view.findViewById(R.id.card_quick_routes);
        btnQuickTerminals = view.findViewById(R.id.card_quick_terminals);
        btnQuickAssisted = view.findViewById(R.id.card_quick_assisted);
        btnQuickSaved = view.findViewById(R.id.card_quick_saved);
        btnQuickDestinations = view.findViewById(R.id.card_quick_destinations);
        btnQuickSafety = view.findViewById(R.id.card_quick_safety);
        cardSearchLimit = view.findViewById(R.id.card_home_search_limit);

        btnOpenAssistant = view.findViewById(R.id.btn_open_assistant_banner);
        tvWelcomeUser = view.findViewById(R.id.tv_home_welcome_user);

        rvHomeSavedRoutes = view.findViewById(R.id.rv_home_saved_routes);
        layoutHomeNoSaved = view.findViewById(R.id.layout_home_no_saved);

        if (tvWelcomeUser != null) {
            tvWelcomeUser.setText("Kumusta, " + sessionManager.getFullName() + " 👋");
        }

        rvHomeSavedRoutes.setLayoutManager(new LinearLayoutManager(requireContext()));
        savedRouteAdapter = new SavedRouteAdapter(null, this);
        rvHomeSavedRoutes.setAdapter(savedRouteAdapter);
    }

    private void setupListeners() {
        btnFindRoute.setOnClickListener(v -> handleFindRoute());

        btnQuickRoute.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(R.id.nav_routes);
            }
        });

        btnQuickTerminals.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(R.id.nav_terminals);
            }
        });

        btnQuickAssisted.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openAssistedNavigation();
            }
        });

        btnQuickSaved.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(R.id.nav_saved);
            }
        });

        if (btnQuickDestinations != null) {
            btnQuickDestinations.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.transitph.app.activities.DestinationDiscoveryActivity.class));
            });
        }

        if (btnQuickSafety != null) {
            btnQuickSafety.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.transitph.app.activities.SafetyReportsActivity.class));
            });
        }

        if (cardSearchLimit != null) {
            cardSearchLimit.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.transitph.app.activities.PremiumUpgradeActivity.class));
            });
        }

        if (btnOpenAssistant != null) {
            btnOpenAssistant.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openAssistedNavigation();
                }
            });
        }
    }

    private void handleFindRoute() {
        String from = etFrom.getText().toString().trim();
        String to = etTo.getText().toString().trim();

        if (from.isEmpty() && to.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter both your starting point and destination.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(requireContext(), RouteResultsActivity.class);
        intent.putExtra(RouteResultsActivity.EXTRA_FROM, from);
        intent.putExtra(RouteResultsActivity.EXTRA_TO, to);
        startActivity(intent);
    }

    private void plotTerminals() {
        // Only terminals that have routes and real coordinates are drawn (small orange dots).
        // The terminals imported from terminals.csv have placeholder coordinates, some of them in the sea.
        List<Terminal> terminals = terminalDao.getAllTerminals();
        TerminalMarkers.addTo(map, terminals);
    }

    private void loadSavedRoutesPreview() {
        long userId = sessionManager.getUserId();
        if (userId > 0) {
            List<SavedRoute> savedList = savedRouteDao.getSavedRoutesForUser(userId);
            if (savedList.isEmpty()) {
                rvHomeSavedRoutes.setVisibility(View.GONE);
                layoutHomeNoSaved.setVisibility(View.VISIBLE);
            } else {
                rvHomeSavedRoutes.setVisibility(View.VISIBLE);
                layoutHomeNoSaved.setVisibility(View.GONE);
                savedRouteAdapter.setSavedRoutes(savedList);
            }
        }
    }

    @Override
    public void onViewRoute(SavedRoute savedRoute) {
        Intent intent = new Intent(requireContext(), RouteDetailsActivity.class);
        intent.putExtra(RouteDetailsActivity.EXTRA_ROUTE_ID, savedRoute.getRouteId());
        startActivity(intent);
    }

    @Override
    public void onDeleteRoute(SavedRoute savedRoute) {
        long userId = sessionManager.getUserId();
        savedRouteDao.deleteSavedRouteById(savedRoute.getId());
        Toast.makeText(requireContext(), "Saved route removed.", Toast.LENGTH_SHORT).show();
        loadSavedRoutesPreview();
    }
}

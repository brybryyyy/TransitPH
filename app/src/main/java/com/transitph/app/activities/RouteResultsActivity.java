package com.transitph.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.transitph.app.utils.SearchQuotaManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.transitph.app.R;
import com.transitph.app.adapters.RouteAdapter;
import com.transitph.app.map.RouteMapHelper;
import com.transitph.app.database.RouteDao;
import com.transitph.app.database.SavedRouteDao;
import com.transitph.app.database.TerminalDao;
import com.transitph.app.models.Route;
import com.transitph.app.models.RouteStop;
import com.transitph.app.models.Terminal;
import com.transitph.app.utils.SessionManager;
import com.transitph.app.utils.RouteTransportFilter;
import com.transitph.app.utils.RouteFarePreference;
import com.google.android.material.chip.ChipGroup;

import org.osmdroid.config.Configuration;
import com.transitph.app.map.TomTomTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class RouteResultsActivity extends AppCompatActivity implements RouteAdapter.OnRouteClickListener {

    public static final String EXTRA_FROM = "extra_from";
    public static final String EXTRA_TO = "extra_to";
    public static final String EXTRA_DESTINATION_LABEL = "extra_destination_label";
    public static final String EXTRA_DESTINATION_FALLBACK = "extra_destination_fallback";
    private static final String EXTRA_QUOTA_ALREADY_CONSUMED = "extra_quota_already_consumed";
    private static final String TEST_REWARDED_AD_UNIT = "ca-app-pub-3940256099942544/5224354917";

    private TextView tvSearchSummary;
    private TextView tvResultCount;
    private TextView tvEmptyRouteDescription;
    private RecyclerView rvRouteResults;
    private View layoutEmptyState;
    private ImageButton btnBack;
    private ChipGroup transportFilters;
    private ChipGroup fareOptionFilters;

    private RouteDao routeDao;
    private SavedRouteDao savedRouteDao;
    private TerminalDao terminalDao;
    private SessionManager sessionManager;
    private RouteAdapter adapter;
    private MapView map;
    private RewardedAd rewardedAd;
    private boolean rewardedAdLoading;
    private boolean searchBlockedByQuota;
    private List<Route> allResults = new ArrayList<>();
    private String selectedTransport = RouteTransportFilter.ALL;

    private String queryFrom = "";
    private String queryTo = "";
    private String destinationLabel = "";
    private String destinationFallback = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_results);

        routeDao = new RouteDao(this);
        savedRouteDao = new SavedRouteDao(this);
        terminalDao = new TerminalDao(this);
        sessionManager = new SessionManager(this);

        queryFrom = getIntent().getStringExtra(EXTRA_FROM);
        queryTo = getIntent().getStringExtra(EXTRA_TO);

        if (queryFrom == null) queryFrom = "";
        if (queryTo == null) queryTo = "";
        destinationLabel = getIntent().getStringExtra(EXTRA_DESTINATION_LABEL);
        destinationFallback = getIntent().getStringExtra(EXTRA_DESTINATION_FALLBACK);
        if (destinationLabel == null) destinationLabel = "";
        if (destinationFallback == null) destinationFallback = "";

        initViews();
        if (savedInstanceState == null && !getIntent().getBooleanExtra(EXTRA_QUOTA_ALREADY_CONSUMED, false)) {
            if (!SearchQuotaManager.tryConsume(this, sessionManager)) {
                searchBlockedByQuota = true;
                showSearchLimitDialog();
                return;
            }
        }
        loadResults();
    }

    private void initViews() {
        tvSearchSummary = findViewById(R.id.tv_search_summary);
        tvResultCount = findViewById(R.id.tv_result_count);
        tvEmptyRouteDescription = findViewById(R.id.tv_empty_route_description);
        rvRouteResults = findViewById(R.id.rv_route_results);
        transportFilters = findViewById(R.id.chips_results_transport_filters);
        fareOptionFilters = findViewById(R.id.chips_results_fare_option);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        btnBack = findViewById(R.id.btn_back_results);

        map = findViewById(R.id.map_results);
        TomTomTileSource.apply(map);   // TomTom tiles
        map.setMultiTouchControls(true);

        btnBack.setOnClickListener(v -> finish());

        rvRouteResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RouteAdapter(null, this);
        rvRouteResults.setAdapter(adapter);
        RouteTransportFilter.setup(this, transportFilters, filter -> {
            selectedTransport = filter;
            applyRouteFilter();
        });
        RouteFarePreference.setup(this, fareOptionFilters, discounted -> {
            if (adapter != null) adapter.notifyDataSetChanged();
        });

        String summary = "";
        if (!queryFrom.isEmpty() && !queryTo.isEmpty()) {
            summary = queryFrom + " → " + queryTo;
        } else if (!queryFrom.isEmpty()) {
            summary = "From " + queryFrom;
        } else if (!queryTo.isEmpty()) {
            summary = destinationLabel.isEmpty() ? "To " + queryTo : "Routes to " + destinationLabel + " (" + queryTo + ")";
        } else {
            summary = "All CALABARZON Routes";
        }
        tvSearchSummary.setText(summary);
    }

    private void showSearchLimitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Daily search limit reached")
                .setMessage("You’ve used all 5 free route searches for today. Upgrade to Premium for unlimited searches, or watch an ad to unlock one more search.")
                .setPositiveButton("Upgrade to Premium", (dialog, which) ->
                        startActivity(new Intent(this, PremiumUpgradeActivity.class)))
                .setNeutralButton("Watch an advertisement", (dialog, which) -> loadAndShowRewardedAd())
                .setNegativeButton("Not now", (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }

    private void loadAndShowRewardedAd() {
        if (rewardedAd != null) {
            showRewardedAd();
            return;
        }
        if (rewardedAdLoading) {
            Toast.makeText(this, "The rewarded ad is loading. Please try again in a moment.", Toast.LENGTH_SHORT).show();
            return;
        }
        rewardedAdLoading = true;
        MobileAds.initialize(getApplicationContext(), status -> runOnUiThread(() -> {
            AdRequest request = new AdRequest.Builder().build();
            RewardedAd.load(this, TEST_REWARDED_AD_UNIT, request, new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(RewardedAd ad) {
                    rewardedAdLoading = false;
                    rewardedAd = ad;
                    showRewardedAd();
                }

                @Override
                public void onAdFailedToLoad(LoadAdError error) {
                    rewardedAdLoading = false;
                    rewardedAd = null;
                    Toast.makeText(RouteResultsActivity.this,
                            "No ad is available right now. Please try again later.", Toast.LENGTH_LONG).show();
                }
            });
        }));
    }

    private void showRewardedAd() {
        RewardedAd ad = rewardedAd;
        if (ad == null) return;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError error) {
                rewardedAd = null;
                Toast.makeText(RouteResultsActivity.this, "The ad could not be shown. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
        ad.show(this, rewardItem -> {
            SearchQuotaManager.addRewardedSearch(RouteResultsActivity.this, sessionManager);
            if (SearchQuotaManager.tryConsume(RouteResultsActivity.this, sessionManager)) {
                Intent retry = new Intent(RouteResultsActivity.this, RouteResultsActivity.class);
                retry.putExtras(getIntent());
                retry.putExtra(EXTRA_QUOTA_ALREADY_CONSUMED, true);
                startActivity(retry);
                finish();
            }
        });
    }
    private void loadResults() {
        allResults = routeDao.searchRoutes(queryFrom, queryTo);
        boolean usingNearbyTerminal = false;
        if (allResults.isEmpty() && queryFrom.isEmpty() && !destinationFallback.isEmpty()) {
            allResults = routeDao.searchRoutes("", destinationFallback);
            usingNearbyTerminal = !allResults.isEmpty();
        }
        if (usingNearbyTerminal) {
            tvSearchSummary.setText("Routes near " + destinationLabel + " via " + destinationFallback);
        }
        applyRouteFilter();
    }

    private void applyRouteFilter() {
        List<Route> routes = RouteTransportFilter.filterRoutes(allResults, selectedTransport);
        String typeLabel = RouteTransportFilter.ALL.equals(selectedTransport) ? "" : " · " + selectedTransport;
        tvResultCount.setText(routes.size() + " route option" + (routes.size() == 1 ? "" : "s") + " shown" + typeLabel);
        if (routes.isEmpty()) {
            rvRouteResults.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (tvEmptyRouteDescription != null && !allResults.isEmpty()) {
                tvEmptyRouteDescription.setText("No " + selectedTransport + " options for this trip. Try another vehicle type.");
            }
        } else {
            rvRouteResults.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            adapter.setRoutes(routes);
            drawRouteOnMap(routes.get(0));
        }
    }

    @Override
    public void onRouteClick(Route route) {
        drawRouteOnMap(route);
        Intent intent = new Intent(this, RouteDetailsActivity.class);
        intent.putExtra(RouteDetailsActivity.EXTRA_ROUTE, route);
        startActivity(intent);
    }

    @Override
    public void onSaveRouteClick(Route route) {
        long userId = sessionManager.getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Please log in to save routes.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (savedRouteDao.isRouteSaved(userId, route.getId())) {
            Toast.makeText(this, "This route is already saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        long res = savedRouteDao.saveRoute(userId, route.getId());
        if (res > 0) {
            Toast.makeText(this, "Route saved successfully.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "This route is already saved.", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawRouteOnMap(Route route) {
        if (map == null) return;
        // Road-following route (TomTom). Drawing logic lives in map/RouteMapHelper.java
        RouteMapHelper.drawRoute(this, map, route, (meters, seconds, roadAccurate, note) -> {
            if (!roadAccurate && note != null) {
                Toast.makeText(this, note, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (map != null) map.onResume();
        if (searchBlockedByQuota && sessionManager != null && sessionManager.isPremiumActive()) {
            searchBlockedByQuota = false;
            loadResults();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}

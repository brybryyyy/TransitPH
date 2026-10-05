package com.transitph.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
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

    private TextView tvSearchSummary;
    private TextView tvResultCount;
    private RecyclerView rvRouteResults;
    private View layoutEmptyState;
    private ImageButton btnBack;

    private RouteDao routeDao;
    private SavedRouteDao savedRouteDao;
    private TerminalDao terminalDao;
    private SessionManager sessionManager;
    private RouteAdapter adapter;
    private MapView map;

    private String queryFrom = "";
    private String queryTo = "";

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

        initViews();
        loadResults();
    }

    private void initViews() {
        tvSearchSummary = findViewById(R.id.tv_search_summary);
        tvResultCount = findViewById(R.id.tv_result_count);
        rvRouteResults = findViewById(R.id.rv_route_results);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        btnBack = findViewById(R.id.btn_back_results);

        map = findViewById(R.id.map_results);
        TomTomTileSource.apply(map);   // TomTom tiles
        map.setMultiTouchControls(true);

        btnBack.setOnClickListener(v -> finish());

        rvRouteResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RouteAdapter(null, this);
        rvRouteResults.setAdapter(adapter);

        String summary = "";
        if (!queryFrom.isEmpty() && !queryTo.isEmpty()) {
            summary = queryFrom + " → " + queryTo;
        } else if (!queryFrom.isEmpty()) {
            summary = "From " + queryFrom;
        } else if (!queryTo.isEmpty()) {
            summary = "To " + queryTo;
        } else {
            summary = "All CALABARZON Routes";
        }
        tvSearchSummary.setText(summary);
    }

    private void loadResults() {
        List<Route> routes = routeDao.searchRoutes(queryFrom, queryTo);
        if (routes.isEmpty()) {
            rvRouteResults.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvResultCount.setText("0 routes found");
        } else {
            rvRouteResults.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            tvResultCount.setText(routes.size() + " route option" + (routes.size() > 1 ? "s" : "") + " available");
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
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}

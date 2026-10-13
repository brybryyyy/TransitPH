package com.transitph.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.transitph.app.R;
import com.transitph.app.activities.RouteDetailsActivity;
import com.transitph.app.activities.RouteResultsActivity;
import com.transitph.app.adapters.RouteAdapter;
import com.transitph.app.database.RouteDao;
import com.transitph.app.database.SavedRouteDao;
import com.transitph.app.database.TerminalDao;
import com.transitph.app.models.Route;
import com.transitph.app.models.Terminal;
import com.transitph.app.utils.SessionManager;
import org.osmdroid.config.Configuration;
import com.transitph.app.map.TerminalMarkers;
import com.transitph.app.map.TomTomTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import android.preference.PreferenceManager;
import java.util.List;

public class RouteFinderFragment extends Fragment implements RouteAdapter.OnRouteClickListener {

    private EditText etFrom;
    private EditText etTo;
    private Button btnFindRoute;
    private ImageButton btnSwap;
    private RecyclerView rvPopularRoutes;

    // Quick chips
    private Button chipCalamba;
    private Button chipSantaRosa;
    private Button chipDasma;
    private Button chipTagaytay;

    private RouteDao routeDao;
    private SavedRouteDao savedRouteDao;
    private TerminalDao terminalDao;
    private SessionManager sessionManager;
    private RouteAdapter adapter;
    private MapView map;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(requireContext()));
        View view = inflater.inflate(R.layout.fragment_route_finder, container, false);

        routeDao = new RouteDao(requireContext());
        savedRouteDao = new SavedRouteDao(requireContext());
        terminalDao = new TerminalDao(requireContext());
        sessionManager = new SessionManager(requireContext());

        initViews(view);
        setupListeners();
        loadFeaturedRoutes();

        return view;
    }

    private void initViews(View view) {
        etFrom = view.findViewById(R.id.et_rf_from);
        etTo = view.findViewById(R.id.et_rf_to);
        btnFindRoute = view.findViewById(R.id.btn_rf_find);
        btnSwap = view.findViewById(R.id.btn_rf_swap);
        rvPopularRoutes = view.findViewById(R.id.rv_rf_popular_routes);

        chipCalamba = view.findViewById(R.id.chip_loc_calamba);
        chipSantaRosa = view.findViewById(R.id.chip_loc_santarosa);
        chipDasma = view.findViewById(R.id.chip_loc_dasma);
        chipTagaytay = view.findViewById(R.id.chip_loc_tagaytay);

        rvPopularRoutes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RouteAdapter(null, this);
        rvPopularRoutes.setAdapter(adapter);

        // Initialize Map
        map = view.findViewById(R.id.map_rf);
        TomTomTileSource.apply(map);   // TomTom tiles
        map.setMultiTouchControls(true);
        map.getController().setZoom(10.0);
        map.getController().setCenter(new GeoPoint(14.2132, 121.1648));

        // Plot terminals: only those with routes and real coordinates (small orange dots)
        List<Terminal> terminals = terminalDao.getAllTerminals();
        TerminalMarkers.addTo(map, terminals);
    }

    private void setupListeners() {
        btnFindRoute.setOnClickListener(v -> executeSearch());

        if (btnSwap != null) {
            btnSwap.setOnClickListener(v -> {
                String temp = etFrom.getText().toString();
                etFrom.setText(etTo.getText().toString());
                etTo.setText(temp);
            });
        }

        if (chipCalamba != null) chipCalamba.setOnClickListener(v -> etFrom.setText("Calamba"));
        if (chipSantaRosa != null) chipSantaRosa.setOnClickListener(v -> etTo.setText("Santa Rosa"));
        if (chipDasma != null) chipDasma.setOnClickListener(v -> etFrom.setText("Dasmariñas"));
        if (chipTagaytay != null) chipTagaytay.setOnClickListener(v -> etTo.setText("Tagaytay"));
    }

    private void executeSearch() {
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

    private void loadFeaturedRoutes() {
        List<Route> routes = routeDao.getAllRoutes();
        if (routes.size() > 8) {
            routes = routes.subList(0, 8);
        }
        adapter.setRoutes(routes);
    }

    @Override
    public void onRouteClick(Route route) {
        Intent intent = new Intent(requireContext(), RouteDetailsActivity.class);
        intent.putExtra(RouteDetailsActivity.EXTRA_ROUTE, route);
        startActivity(intent);
    }

    @Override
    public void onSaveRouteClick(Route route) {
        long userId = sessionManager.getUserId();
        if (userId <= 0) {
            Toast.makeText(requireContext(), "Please log in to save routes.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (savedRouteDao.isRouteSaved(userId, route.getId())) {
            Toast.makeText(requireContext(), "This route is already saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        long res = savedRouteDao.saveRoute(userId, route.getId());
        if (res > 0) {
            Toast.makeText(requireContext(), "Route saved successfully.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "This route is already saved.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}

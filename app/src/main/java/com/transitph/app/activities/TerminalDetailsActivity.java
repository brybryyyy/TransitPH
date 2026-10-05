package com.transitph.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.transitph.app.R;
import com.transitph.app.adapters.RouteAdapter;
import com.transitph.app.database.RouteDao;
import com.transitph.app.database.SavedRouteDao;
import com.transitph.app.database.TerminalDao;
import com.transitph.app.models.Route;
import com.transitph.app.models.Terminal;
import com.transitph.app.utils.SessionManager;
import java.util.List;

public class TerminalDetailsActivity extends AppCompatActivity implements RouteAdapter.OnRouteClickListener {

    public static final String EXTRA_TERMINAL = "extra_terminal";
    public static final String EXTRA_TERMINAL_ID = "extra_terminal_id";

    private TextView tvTerminalName;
    private TextView tvLocation;
    private TextView tvCoordinates;
    private TextView tvDescription;
    private TextView tvRoutesCountHeader;
    private RecyclerView rvAvailableRoutes;
    private View layoutNoRoutes;
    private Button btnNavigate;
    private ImageButton btnBack;

    private Terminal terminal;
    private TerminalDao terminalDao;
    private RouteDao routeDao;
    private SavedRouteDao savedRouteDao;
    private SessionManager sessionManager;
    private RouteAdapter routeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal_details);

        terminalDao = new TerminalDao(this);
        routeDao = new RouteDao(this);
        savedRouteDao = new SavedRouteDao(this);
        sessionManager = new SessionManager(this);

        terminal = (Terminal) getIntent().getSerializableExtra(EXTRA_TERMINAL);
        if (terminal == null) {
            long terminalId = getIntent().getLongExtra(EXTRA_TERMINAL_ID, -1);
            if (terminalId > 0) {
                terminal = terminalDao.getTerminalById(terminalId);
            }
        }

        if (terminal == null) {
            Toast.makeText(this, "Terminal not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadTerminalData();
    }

    private void initViews() {
        tvTerminalName = findViewById(R.id.tv_td_name);
        tvLocation = findViewById(R.id.tv_td_location);
        tvCoordinates = findViewById(R.id.tv_td_coordinates);
        tvDescription = findViewById(R.id.tv_td_description);
        tvRoutesCountHeader = findViewById(R.id.tv_td_routes_header);
        rvAvailableRoutes = findViewById(R.id.rv_terminal_routes);
        layoutNoRoutes = findViewById(R.id.layout_td_no_routes);
        btnNavigate = findViewById(R.id.btn_td_navigate);
        btnBack = findViewById(R.id.btn_back_terminal_details);

        btnBack.setOnClickListener(v -> finish());

        rvAvailableRoutes.setLayoutManager(new LinearLayoutManager(this));
        routeAdapter = new RouteAdapter(null, this);
        rvAvailableRoutes.setAdapter(routeAdapter);

        btnNavigate.setOnClickListener(v -> {
            Intent mapIntent = new Intent(this, MapActivity.class);
            mapIntent.putExtra(MapActivity.EXTRA_TERMINAL, terminal);
            startActivity(mapIntent);
        });
    }

    private void loadTerminalData() {
        tvTerminalName.setText(terminal.getTerminalName());
        tvLocation.setText("📍 " + terminal.getLocationText());
        tvCoordinates.setText("🌐 GPS: " + terminal.getCoordinatesText());
        tvDescription.setText(terminal.getDescription() != null ? terminal.getDescription() : "Standard jeepney and provincial transit terminal.");

        List<Route> routes = routeDao.getRoutesByTerminalId(terminal.getId());
        tvRoutesCountHeader.setText("AVAILABLE ROUTES (" + routes.size() + ")");

        if (routes.isEmpty()) {
            rvAvailableRoutes.setVisibility(View.GONE);
            layoutNoRoutes.setVisibility(View.VISIBLE);
        } else {
            rvAvailableRoutes.setVisibility(View.VISIBLE);
            layoutNoRoutes.setVisibility(View.GONE);
            routeAdapter.setRoutes(routes);
        }
    }

    @Override
    public void onRouteClick(Route route) {
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
}

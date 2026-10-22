package com.transitph.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.transitph.app.R;
import com.transitph.app.adapters.RouteAdapter;
import com.transitph.app.database.RouteDao;
import com.transitph.app.database.SavedRouteDao;
import com.transitph.app.database.TerminalDao;
import com.transitph.app.models.Route;
import com.transitph.app.utils.RouteFarePreference;
import com.transitph.app.models.Terminal;
import com.transitph.app.utils.SessionManager;
import com.transitph.app.utils.RouteTransportFilter;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

public class TerminalDetailsActivity extends AppCompatActivity implements RouteAdapter.OnRouteClickListener {

    public static final String EXTRA_TERMINAL = "extra_terminal";
    public static final String EXTRA_TERMINAL_ID = "extra_terminal_id";

    private TextView tvTerminalName;
    private TextView tvLocation;
    private TextView tvCoordinates;
    private TextView tvDescription;
    private TextView tvRoutesCountHeader;
    private LinearLayout routeListContainer;
    private View layoutNoRoutes;
    private Button btnNavigate;
    private ImageButton btnBack;
    private ChipGroup transportFilters;
    private TextView tvNoRoutesMessage;

    private Terminal terminal;
    private TerminalDao terminalDao;
    private RouteDao routeDao;
    private SavedRouteDao savedRouteDao;
    private SessionManager sessionManager;
    private List<Route> allRoutes = new java.util.ArrayList<>();
    private String selectedTransport = RouteTransportFilter.ALL;

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
        routeListContainer = findViewById(R.id.rv_terminal_routes);
        layoutNoRoutes = findViewById(R.id.layout_td_no_routes);
        btnNavigate = findViewById(R.id.btn_td_navigate);
        btnBack = findViewById(R.id.btn_back_terminal_details);
        transportFilters = findViewById(R.id.chips_td_transport_filters);
        tvNoRoutesMessage = findViewById(R.id.tv_td_no_routes_message);

        btnBack.setOnClickListener(v -> finish());
        RouteTransportFilter.setup(this, transportFilters, filter -> {
            selectedTransport = filter;
            renderRoutes();
        });

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

        allRoutes = routeDao.getRoutesByTerminalId(terminal.getId());
        renderRoutes();
    }

    private void renderRoutes() {
        List<Route> routes = RouteTransportFilter.filterRoutes(allRoutes, selectedTransport);
        tvRoutesCountHeader.setText("AVAILABLE ROUTES (" + routes.size() + (routes.size() == allRoutes.size() ? ")" : " of " + allRoutes.size() + ")"));
        routeListContainer.removeAllViews();

        if (routes.isEmpty()) {
            routeListContainer.setVisibility(View.GONE);
            layoutNoRoutes.setVisibility(View.VISIBLE);
            if (tvNoRoutesMessage != null) {
                tvNoRoutesMessage.setText(allRoutes.isEmpty()
                        ? "No routes currently registered for this terminal."
                        : "No " + selectedTransport + " routes here. Choose All to see every route.");
            }
        } else {
            routeListContainer.setVisibility(View.VISIBLE);
            layoutNoRoutes.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(this);
            for (Route route : routes) {
                View routeCard = inflater.inflate(R.layout.item_route, routeListContainer, false);
                bindRouteCard(routeCard, route);
                routeListContainer.addView(routeCard);
            }
        }
    }

    private void bindRouteCard(View itemView, Route route) {
        TextView tvTransportBadge = itemView.findViewById(R.id.tv_transport_badge);
        TextView tvRouteTitle = itemView.findViewById(R.id.tv_route_title);
        TextView tvTerminalName = itemView.findViewById(R.id.tv_terminal_name);
        TextView tvFare = itemView.findViewById(R.id.tv_fare);
        TextView tvTravelTime = itemView.findViewById(R.id.tv_travel_time);
        TextView tvTransfers = itemView.findViewById(R.id.tv_transfers);
        TextView tvWalking = itemView.findViewById(R.id.tv_walking);
        Button btnViewRoute = itemView.findViewById(R.id.btn_view_route);
        Button btnSaveRoute = itemView.findViewById(R.id.btn_save_route);

        tvTransportBadge.setText(route.getTransportIconText());
        tvRouteTitle.setText(route.getOrigin() + " → " + route.getDestination());
        tvTerminalName.setText(route.getTerminalName() != null ? "📍 " + route.getTerminalName() : "📍 Terminal");
        tvFare.setText(RouteFarePreference.formatFare(itemView.getContext(), route));
        tvTravelTime.setText(route.getFormattedTime());
        tvTransfers.setText(route.getTransferCount() + " transfer");
        tvWalking.setText("🚶 " + route.getWalkingDistanceMeters() + "m walking");

        itemView.setOnClickListener(v -> onRouteClick(route));
        btnViewRoute.setOnClickListener(v -> onRouteClick(route));
        btnSaveRoute.setOnClickListener(v -> onSaveRouteClick(route));
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

package com.transitph.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.transitph.app.R;
import com.transitph.app.adapters.TimelineAdapter;
import com.transitph.app.database.RouteDao;
import com.transitph.app.database.SavedRouteDao;
import com.transitph.app.models.Route;
import com.transitph.app.models.RouteStop;
import com.transitph.app.services.RouteSearchService;
import com.transitph.app.services.RouteSearchService.TimelineStep;
import com.transitph.app.utils.SessionManager;
import java.util.List;

public class RouteDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_ROUTE = "extra_route";
    public static final String EXTRA_ROUTE_ID = "extra_route_id";

    private TextView tvRouteName;
    private TextView tvTransportType;
    private TextView tvFromTo;
    private TextView tvTerminal;
    private TextView tvFare;
    private TextView tvTravelTime;
    private TextView tvDescription;
    private TextView tvWeatherTemp;
    private TextView tvWeatherAdvisoryText;
    private TextView tvSafetyScore;
    private TextView tvSafetyAssessmentText;
    private RecyclerView rvTimeline;
    private Spinner spLanguage;
    private Button btnSaveRoute;
    private Button btnViewOnMap;
    private ImageButton btnBack;

    private Route route;
    private RouteDao routeDao;
    private SavedRouteDao savedRouteDao;
    private SessionManager sessionManager;
    private TimelineAdapter timelineAdapter;
    private List<TimelineStep> timelineSteps;
    private String selectedLanguage = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_details);

        routeDao = new RouteDao(this);
        savedRouteDao = new SavedRouteDao(this);
        sessionManager = new SessionManager(this);

        route = (Route) getIntent().getSerializableExtra(EXTRA_ROUTE);
        if (route == null) {
            long routeId = getIntent().getLongExtra(EXTRA_ROUTE_ID, -1);
            if (routeId > 0) {
                route = routeDao.getRouteById(routeId);
            }
        }

        if (route == null) {
            Toast.makeText(this, "Route not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindRouteData();
    }

    private void initViews() {
        tvRouteName = findViewById(R.id.tv_details_route_name);
        tvTransportType = findViewById(R.id.tv_details_transport_type);
        tvFromTo = findViewById(R.id.tv_details_from_to);
        tvTerminal = findViewById(R.id.tv_details_terminal);
        tvFare = findViewById(R.id.tv_details_fare);
        tvTravelTime = findViewById(R.id.tv_details_travel_time);
        tvDescription = findViewById(R.id.tv_details_description);
        tvWeatherTemp = findViewById(R.id.tv_weather_temp);
        tvWeatherAdvisoryText = findViewById(R.id.tv_weather_advisory_text);
        tvSafetyScore = findViewById(R.id.tv_safety_score);
        tvSafetyAssessmentText = findViewById(R.id.tv_safety_assessment_text);
        rvTimeline = findViewById(R.id.rv_route_timeline);
        spLanguage = findViewById(R.id.sp_instruction_language);
        btnSaveRoute = findViewById(R.id.btn_save_route_details);
        btnViewOnMap = findViewById(R.id.btn_view_on_map);
        btnBack = findViewById(R.id.btn_back_details);

        btnBack.setOnClickListener(v -> finish());
        
        btnViewOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra(MapActivity.EXTRA_ROUTE, route);
            startActivity(intent);
        });

        rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        selectedLanguage = "fil".equalsIgnoreCase(sessionManager.getSelectedLanguage()) ? "fil" : "en";
        timelineSteps = RouteSearchService.generateTimelineSteps(route);
        timelineAdapter = new TimelineAdapter(timelineSteps, selectedLanguage);
        rvTimeline.setAdapter(timelineAdapter);

        setupLanguageSpinner();

        btnSaveRoute.setOnClickListener(v -> handleSaveRoute());
    }

    private void setupLanguageSpinner() {
        final String[] labels = {"English", "Filipino"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.item_language_spinner, labels) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setText("🌐  " + labels[position] + "  ▾");
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setText(labels[position]);
                return v;
            }
        };
        spLanguage.setAdapter(adapter);
        spLanguage.setSelection("fil".equals(selectedLanguage) ? 1 : 0, false);

        spLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String lang = (position == 1) ? "fil" : "en";
                if (!lang.equals(selectedLanguage)) {
                    selectedLanguage = lang;
                    sessionManager.setSelectedLanguage(lang);
                    timelineAdapter.updateSteps(timelineSteps, selectedLanguage);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void bindRouteData() {
        tvRouteName.setText(route.getRouteName());
        tvTransportType.setText(route.getTransportIconText());
        tvFromTo.setText(route.getOrigin() + " ➔ " + route.getDestination());
        tvTerminal.setText("📍 " + (route.getTerminalName() != null ? route.getTerminalName() : "Local Terminal"));
        tvFare.setText(route.getFormattedFare());
        tvTravelTime.setText("⏱ " + route.getFormattedTime());
        tvDescription.setText(route.getDescription() != null ? route.getDescription() : "Standard commute route in CALABARZON.");

        // Demo values (not live data yet)
        tvWeatherTemp.setText("29°C");
        tvWeatherAdvisoryText.setText("Partly cloudy");
        tvSafetyScore.setText("92/100");
        tvSafetyAssessmentText.setText("High visibility route");

        // Check if already saved
        long userId = sessionManager.getUserId();
        if (userId > 0 && savedRouteDao.isRouteSaved(userId, route.getId())) {
            btnSaveRoute.setText("✓ SAVED");
            btnSaveRoute.setEnabled(false);
        }
    }

    private void handleSaveRoute() {
        long userId = sessionManager.getUserId();
        if (userId <= 0) {
            Toast.makeText(this, "Please log in to save routes.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (savedRouteDao.isRouteSaved(userId, route.getId())) {
            Toast.makeText(this, "This route is already saved.", Toast.LENGTH_SHORT).show();
            btnSaveRoute.setText("✓ SAVED");
            btnSaveRoute.setEnabled(false);
            return;
        }

        long res = savedRouteDao.saveRoute(userId, route.getId());
        if (res > 0) {
            Toast.makeText(this, "Route saved successfully.", Toast.LENGTH_SHORT).show();
            btnSaveRoute.setText("✓ SAVED");
            btnSaveRoute.setEnabled(false);
        } else {
            Toast.makeText(this, "This route is already saved.", Toast.LENGTH_SHORT).show();
        }
    }
}

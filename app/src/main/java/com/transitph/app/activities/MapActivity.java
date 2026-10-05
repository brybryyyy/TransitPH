package com.transitph.app.activities;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.transitph.app.R;
import com.transitph.app.database.TerminalDao;
import com.transitph.app.map.RouteMapHelper;
import com.transitph.app.map.TerminalMarkers;
import com.transitph.app.map.TomTomTileSource;
import com.transitph.app.models.Route;
import com.transitph.app.models.Terminal;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

/**
 * Full-screen map with three modes:
 *  - EXTRA_TERMINAL : zoomed in on one terminal (opened from Terminal Overview > Navigate)
 *  - EXTRA_ROUTE    : a route drawn along real roads (opened from Route Details > View on map)
 *  - neither        : all terminals that have routes
 */
public class MapActivity extends AppCompatActivity {

    public static final String EXTRA_ROUTE = "extra_route";
    public static final String EXTRA_TERMINAL = "extra_terminal";

    private MapView map;
    private TerminalDao terminalDao;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_map);

        terminalDao = new TerminalDao(this);
        Route route = (Route) getIntent().getSerializableExtra(EXTRA_ROUTE);
        Terminal terminal = (Terminal) getIntent().getSerializableExtra(EXTRA_TERMINAL);

        map = findViewById(R.id.mapview);
        TomTomTileSource.apply(map);
        map.setMultiTouchControls(true);

        ImageButton btnBack = findViewById(R.id.btn_map_back);
        btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tv_map_title);
        tvStatus = findViewById(R.id.tv_map_status);

        map.getController().setZoom(10.0);
        map.getController().setCenter(new GeoPoint(14.2132, 121.1648));

        if (terminal != null) {
            tvTitle.setText(terminal.getTerminalName());
            tvStatus.setText(terminal.getCity() + ", " + terminal.getProvince());
            showTerminal(terminal);
        } else if (route != null) {
            tvTitle.setText("Route: " + route.getRouteName());
            tvStatus.setText("Finding road route…");
            RouteMapHelper.drawRoute(this, map, route, (meters, seconds, roadAccurate, note) -> {
                if (roadAccurate) {
                    tvStatus.setText(RouteMapHelper.formatSummary(meters, seconds));
                } else {
                    tvStatus.setText("Approximate route");
                    if (note != null) Toast.makeText(this, note, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            TerminalMarkers.addTo(map, terminalDao.getAllTerminals());
        }
    }

    private void showTerminal(Terminal t) {
        if (t.getLatitude() == 0 || t.getLongitude() == 0) {
            Toast.makeText(this, "This terminal has no map location yet.", Toast.LENGTH_LONG).show();
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        int px = (int) (22 * d);

        GradientDrawable pin = new GradientDrawable();
        pin.setShape(GradientDrawable.OVAL);
        pin.setColor(Color.parseColor("#D32F2F"));
        pin.setStroke((int) (3 * d), Color.WHITE);
        pin.setSize(px, px);
        pin.setBounds(0, 0, px, px);

        GeoPoint p = new GeoPoint(t.getLatitude(), t.getLongitude());
        Marker m = new Marker(map);
        m.setPosition(p);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        m.setTitle(t.getTerminalName());
        m.setSnippet(t.getCity() + ", " + t.getProvince());
        m.setIcon(pin);
        map.getOverlays().add(m);
        m.showInfoWindow();

        map.getController().setZoom(17.0);
        map.getController().setCenter(p);
        map.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}

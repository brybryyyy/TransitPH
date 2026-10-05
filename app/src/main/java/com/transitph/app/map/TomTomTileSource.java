package com.transitph.app.map;

import com.transitph.app.BuildConfig;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;

/**
 * Loads map tiles from TomTom instead of OpenStreetMap.
 *
 * Usage (one line per map screen):
 *     TomTomTileSource.apply(map);
 *
 * To try TomTom's newer "Orbis" maps later, only BASE_URL and getTileURLString() change.
 */
public class TomTomTileSource extends OnlineTileSourceBase {

    // TomTom Maps Raster Tile API (v1, "basic" layer, "main" style)
    private static final String BASE_URL = "https://api.tomtom.com/map/1/tile/basic/main/";

    public TomTomTileSource() {
        super("TomTom", 0, 19, 256, ".png", new String[]{BASE_URL}, "© TomTom");
    }

    @Override
    public String getTileURLString(long tileIndex) {
        return getBaseUrl()
                + MapTileIndex.getZoom(tileIndex) + "/"
                + MapTileIndex.getX(tileIndex) + "/"
                + MapTileIndex.getY(tileIndex)
                + mImageFilenameEnding
                + "?key=" + BuildConfig.TOMTOM_API_KEY;
    }

    /** Switches a MapView to TomTom tiles and adds the required "© TomTom" attribution. */
    public static void apply(MapView map) {
        Configuration.getInstance().setUserAgentValue(map.getContext().getPackageName());
        map.setTileSource(new TomTomTileSource());
        map.getOverlays().add(new CopyrightOverlay(map.getContext()));
    }
}

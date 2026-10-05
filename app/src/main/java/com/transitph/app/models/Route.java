package com.transitph.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Route implements Serializable {
    private long id;
    private long terminalId;
    private String terminalName;
    private String routeName;
    private String origin;
    private String destination;
    private String transportType; // Jeepney, Bus, Walking, Multi-modal
    private double fare;
    private int estimatedTravelTime; // in minutes
    private String description;
    private int transferCount;
    private int walkingDistanceMeters;
    private List<RouteStop> stops = new ArrayList<>();
    private String timelineJson; // step-by-step instructions stored in Supabase (optional)

    public Route() {
        this.transferCount = 1;
        this.walkingDistanceMeters = 400;
    }

    public Route(long id, long terminalId, String routeName, String origin, String destination,
                 String transportType, double fare, int estimatedTravelTime, String description) {
        this.id = id;
        this.terminalId = terminalId;
        this.routeName = routeName;
        this.origin = origin;
        this.destination = destination;
        this.transportType = transportType;
        this.fare = fare;
        this.estimatedTravelTime = estimatedTravelTime;
        this.description = description;
        this.transferCount = 1;
        this.walkingDistanceMeters = 450;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getTerminalId() { return terminalId; }
    public void setTerminalId(long terminalId) { this.terminalId = terminalId; }

    public String getTerminalName() { return terminalName; }
    public void setTerminalName(String terminalName) { this.terminalName = terminalName; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getTransportType() { return transportType; }
    public void setTransportType(String transportType) { this.transportType = transportType; }

    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }

    public int getEstimatedTravelTime() { return estimatedTravelTime; }
    public void setEstimatedTravelTime(int estimatedTravelTime) { this.estimatedTravelTime = estimatedTravelTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getTransferCount() { return transferCount; }
    public void setTransferCount(int transferCount) { this.transferCount = transferCount; }

    public int getWalkingDistanceMeters() { return walkingDistanceMeters; }
    public void setWalkingDistanceMeters(int walkingDistanceMeters) { this.walkingDistanceMeters = walkingDistanceMeters; }

    public List<RouteStop> getStops() { return stops; }
    public void setStops(List<RouteStop> stops) { this.stops = stops; }

    public String getTimelineJson() { return timelineJson; }
    public void setTimelineJson(String timelineJson) { this.timelineJson = timelineJson; }

    public String getFormattedFare() {
        return String.format(java.util.Locale.US, "₱%.2f", fare);
    }

    public String getFormattedTime() {
        return estimatedTravelTime + " min";
    }

    public String getTransportIconText() {
        if ("Bus".equalsIgnoreCase(transportType)) return "🚌 BUS";
        if ("Walking".equalsIgnoreCase(transportType)) return "🚶 WALKING";
        if ("UV Express".equalsIgnoreCase(transportType)) return "🚐 UV EXPRESS";
        if ("Modern Jeepney".equalsIgnoreCase(transportType)) return "🚐 MODERN JEEPNEY";
        return "🚐 JEEPNEY";
    }
}

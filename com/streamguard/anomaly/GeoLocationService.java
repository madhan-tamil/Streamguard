package com.streamguard.anomaly;

import java.util.HashMap;
import java.util.Map;

/**
 * GeoLocationService.java
 * Provides geographical coordinates and calculates Haversine distance and travel speed.
 *
 * Used for:
 *   - "Impossible Travel" detection (card cloning detection)
 *   - Travel feasibility analysis between consecutive transactions
 *
 * Why this is superior to static location matching:
 *   - Old rule: Flags any transaction outside registered city as fraud (illogical when people travel).
 *   - New Anomaly Engine: Allows travel as long as speed is physically possible
 *     (e.g., Chennai to Bangalore in 5 hours = 58 km/h -> SAFE).
 *     Flags when travel is physically impossible
 *     (e.g., Chennai to Delhi in 30 minutes = 3,520 km/h -> FRAUD / Cloned Card).
 *
 * Project    : StreamGuard
 * Team       : MADHAN T (210425149028), AVINASH N T (210425149006)
 * Department : CSBS, Chennai Institute of Technology
 */
public class GeoLocationService {

    // Radius of Earth in kilometers
    private static final double EARTH_RADIUS_KM = 6371.0;

    // Maximum physically possible commercial travel speed in km/h (passenger plane ~850 km/h)
    public static final double MAX_FEASIBLE_SPEED_KMH = 850.0;

    public static class Coordinates {
        public final double latitude;
        public final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private static final Map<String, Coordinates> CITY_COORDINATES = new HashMap<>();

    static {
        // Major Indian Cities Coordinates (Latitude, Longitude)
        CITY_COORDINATES.put("chennai",    new Coordinates(13.0827, 80.2707));
        CITY_COORDINATES.put("bangalore",  new Coordinates(12.9716, 77.5946));
        CITY_COORDINATES.put("bengaluru",  new Coordinates(12.9716, 77.5946));
        CITY_COORDINATES.put("coimbatore", new Coordinates(11.0168, 76.9558));
        CITY_COORDINATES.put("mumbai",     new Coordinates(19.0760, 72.8777));
        CITY_COORDINATES.put("delhi",      new Coordinates(28.7041, 77.1025));
        CITY_COORDINATES.put("new delhi",  new Coordinates(28.7041, 77.1025));
        CITY_COORDINATES.put("hyderabad",  new Coordinates(17.3850, 78.4867));
        CITY_COORDINATES.put("kolkata",    new Coordinates(22.5726, 88.3639));
        CITY_COORDINATES.put("pune",       new Coordinates(18.5204, 73.8567));
        CITY_COORDINATES.put("kochi",      new Coordinates(9.9312,  76.2673));
        CITY_COORDINATES.put("madurai",    new Coordinates(9.9252,  78.1198));
        CITY_COORDINATES.put("trichy",     new Coordinates(10.7905, 78.7047));
        CITY_COORDINATES.put("salem",      new Coordinates(11.6643, 78.1460));
        CITY_COORDINATES.put("jaipur",     new Coordinates(26.9124, 75.7873));
        CITY_COORDINATES.put("ahmedabad",  new Coordinates(23.0225, 72.5714));
    }

    /**
     * Retrieves coordinates for a city name (case-insensitive).
     */
    public static Coordinates getCoordinates(String cityName) {
        if (cityName == null) return null;
        return CITY_COORDINATES.get(cityName.trim().toLowerCase());
    }

    /**
     * Calculates great-circle distance between two cities using the Haversine formula.
     *
     * @param cityA Source city name
     * @param cityB Destination city name
     * @return Distance in kilometers, or -1 if a city is unknown
     */
    public static double calculateDistanceKm(String cityA, String cityB) {
        if (cityA == null || cityB == null) return 0.0;
        if (cityA.trim().equalsIgnoreCase(cityB.trim())) return 0.0;

        Coordinates c1 = getCoordinates(cityA);
        Coordinates c2 = getCoordinates(cityB);

        // If either city is unknown, assume approximate regional distance
        if (c1 == null || c2 == null) {
            return 350.0; // default conservative estimate for unknown domestic city
        }

        double lat1Rad = Math.toRadians(c1.latitude);
        double lat2Rad = Math.toRadians(c2.latitude);
        double deltaLat = Math.toRadians(c2.latitude - c1.latitude);
        double deltaLon = Math.toRadians(c2.longitude - c1.longitude);

        double a = Math.sin(deltaLat / 2.0) * Math.sin(deltaLat / 2.0)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2.0) * Math.sin(deltaLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Calculates speed of movement between two locations given time difference in minutes.
     *
     * @param distanceKm Distance traversed
     * @param minutesElapsed Time in minutes between events
     * @return Speed in km/h
     */
    public static double calculateSpeedKmh(double distanceKm, double minutesElapsed) {
        if (minutesElapsed <= 0.0) {
            return distanceKm > 0.0 ? 99999.0 : 0.0; // Instantaneous teleportation
        }
        double hoursElapsed = minutesElapsed / 60.0;
        return distanceKm / hoursElapsed;
    }
}


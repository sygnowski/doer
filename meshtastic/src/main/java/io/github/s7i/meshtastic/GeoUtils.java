package io.github.s7i.meshtastic;


public class GeoUtils {


    public static double latLongToMeter(
          double lat_a,
          double lng_a,
          double lat_b,
          double lng_b
    ) {
        return haversineDistance(lat_a, lng_a, lat_b, lng_b);
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // mean radius of the earth in meters

        double latRad1 = Math.toRadians(lat1);
        double latRad2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
              + Math.cos(latRad1) * Math.cos(latRad2)
              * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

}

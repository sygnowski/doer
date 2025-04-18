package io.github.s7i.meshtastic;

import static java.lang.Math.PI;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class GeoUtils {


    public static double latLongToMeter(
          double lat_a,
          double lng_a,
          double lat_b,
          double lng_b
    ) {
        var pk = (180 / PI);
        var a1 = lat_a / pk;
        var a2 = lng_a / pk;
        var b1 = lat_b / pk;
        var b2 = lng_b / pk;
        var t1 = cos(a1) * cos(a2) * cos(b1) * cos(b2);
        var t2 = cos(a1) * sin(a2) * cos(b1) * sin(b2);
        var t3 = sin(a1) * sin(b1);
        var tt = acos(t1 + t2 + t3);
        if (Double.isNaN(tt)) {
            tt = 0.0;// Must have been the same point?
        }
        return 6366000 * tt;
    }
}

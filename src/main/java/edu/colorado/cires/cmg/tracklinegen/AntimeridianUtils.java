package edu.colorado.cires.cmg.tracklinegen;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.jetbrains.annotations.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

public final class AntimeridianUtils {

  private static final double METERS_PER_NMILE = 1852D;
  private static final double SECONDS_PER_HOUR = 3600D;
  private static final double KN_PER_MPS = SECONDS_PER_HOUR / METERS_PER_NMILE;

  private AntimeridianUtils(){

  }

  private static boolean signsEqual(double s1, double s2) {
   return ((s1 < 0D && s2 < 0D) || (s1 > 0D && s2 > 0D) || (s1 == 0D && s2 == 0D));
  }

  private static double mpsToKnots(double metersPerSecond) {
    return metersPerSecond * KN_PER_MPS;
  }

  public static double getSpeed(double maxAllowedSpeedKnts, Coordinate c1, Coordinate c2, double m) throws ValidationException {
    return Objects.requireNonNull(getSpeed(maxAllowedSpeedKnts, c1, c2, m, false));
  }

  public static @Nullable Double getSpeed(double maxAllowedSpeedKnts, Coordinate c1, Coordinate c2, double m, boolean allowDuplicateTimestamps) throws ValidationException {
    double time2 = c2.getZ();
    double time1 = c1.getZ();

    if (allowDuplicateTimestamps && time2 == time1) {
      return null;
    }

    double s = (time2 - time1) / 1000D;
    if (s == 0D && m == 0D) {
        return 0D;  //allow duplicate points
    }
    double metersPerSecond = m / s;

    double knots = mpsToKnots(metersPerSecond);
    if (maxAllowedSpeedKnts > 0 && knots > maxAllowedSpeedKnts) {
      throw new ValidationException(
          String.format("Speed from (%f, %f, %s) to (%f, %f, %s) was %f knots, which exceeded allowed maximum of %f knots",
              c1.getX(), c1.getY(), Instant.ofEpochMilli((long) c1.getZ()),
              c2.getX(), c2.getY(), Instant.ofEpochMilli((long) c2.getZ()),
              knots,
              maxAllowedSpeedKnts
          ));
    }
    return metersPerSecond;
  }

  private static boolean is180(Coordinate coordinate) {
    return coordinate.getX() == 180D || coordinate.getX() == -180D;
  }

  public static List<Coordinate> splitAm(Coordinate last,  Coordinate current, GeometryFactory geometryFactory) {

    double lastSign = Math.signum(last.getX());
    double currentSign = Math.signum(current.getX());

    if (is180(last)) {
      if (is180(current)) {
        return Collections.singletonList(new Coordinate(last.getX(), current.getY(), current.getZ()));
      }
      if(signsEqual(lastSign, currentSign)) {
        return Collections.singletonList(current);
      }
      return Arrays.asList(
          current,
          new Coordinate(180D * lastSign, last.getY()),
          new Coordinate(180D * currentSign, last.getY())
      );
    }
    if (is180(current)) {
      return Collections.singletonList(new Coordinate(180D * lastSign, current.getY(), current.getZ()));
    }

    List<Coordinate> split;
    if ((current.getX() < 0 && last.getX() > 0) || (current.getX() > 0 && last.getX() < 0)) {
      LineString lineString = geometryFactory.createLineString(new Coordinate[]{last, current});
      Geometry geometry = new JtsGeometry(lineString, JtsSpatialContext.GEO, true, true).getGeom();
      if (geometry instanceof LineString) {
        return Collections.singletonList(current);
      } else if (geometry instanceof GeometryCollection) {
        split = geometryParse(current, last, (GeometryCollection) geometry);
      } else {
        throw new IllegalStateException(
            String.format("An error occurred splitting AM, type: %s from coordinates (%f, %f, %s) to (%f, %f, %s)",
                geometry.toString(),
                current.getX(), current.getY(), Instant.ofEpochMilli((long) current.getZ()),
                last.getX(), last.getY(), Instant.ofEpochMilli((long) last.getZ())
            ));
      }
    } else {
      split = Collections.singletonList(current);
    }
    return split;
  }
  private static Coordinate resolveCoordinate(LineString lineString, Coordinate last) {
    if (lineString.getCoordinateN(0).equals(last)) {
      return lineString.getCoordinateN(1);
    }
    return lineString.getCoordinateN(0);
  }
  private static List<Coordinate> geometryParse(Coordinate coordinate, Coordinate last, GeometryCollection geometry) {
    Geometry l1 = geometry.getGeometryN(0);
    Geometry l2 = geometry.getGeometryN(1);

    if (l1 instanceof LineString && l2 instanceof LineString) {
      return geometryParse(coordinate, last, (LineString) l1, (LineString) l2, geometry);
    } else if (l1 instanceof LineString && l2 instanceof Point) {
      return Collections.singletonList(resolveCoordinate((LineString) l1, last));
    } else if (l2 instanceof LineString && l1 instanceof Point) {
      return Collections.singletonList(resolveCoordinate((LineString) l2, last));
    } else {
      throw new IllegalStateException(
          String.format("An error occurred splitting AM, type: %s from coordinates (%f, %f, %s) to (%f, %f, %s)",
              geometry.toString(),
              coordinate.getX(), coordinate.getY(), Instant.ofEpochMilli((long) coordinate.getZ()),
              last.getX(), last.getY(), Instant.ofEpochMilli((long) last.getZ())
          ));
    }
  }

  private static List<Coordinate> geometryParse(Coordinate coordinate, Coordinate last, LineString l1, LineString l2, Geometry geometry) {
    List<Coordinate> split = new ArrayList<>(4);
    if (l1.getCoordinateN(0).equals(last) || l2.getCoordinateN(1).equals(coordinate)) {
      split.add(l1.getCoordinateN(0));
      split.add(l1.getCoordinateN(1));
      split.add(l2.getCoordinateN(0));
      split.add(l2.getCoordinateN(1));
    } else if (l2.getCoordinateN(0).equals(last) || l1.getCoordinateN(1).equals(coordinate)) {
      split.add(l2.getCoordinateN(0));
      split.add(l2.getCoordinateN(1));
      split.add(l1.getCoordinateN(0));
      split.add(l1.getCoordinateN(1));
    } else {
      throw new IllegalStateException("Unable to determine AM split order: " + geometry +
          " coordinate: " + coordinate + " last: " + last + " L1: " + l1 + " l2: " + l2);
    }
    return split;
  }

  public static double getDistance(Coordinate c1, Coordinate c2) {
    GeodeticCalculator calc = new GeodeticCalculator(DefaultEllipsoid.WGS84);
    calc.setStartingGeographicPoint(c1.getX(), c1.getY());
    calc.setDestinationGeographicPoint(c2.getX(), c2.getY());
    return calc.getOrthodromicDistance();
  }
}

package edu.colorado.cires.cmg.tracklinegen;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

public final class AntimeridianUtils {
  private AntimeridianUtils(){

  }
  public static List<Coordinate> splitAm(Coordinate last, Coordinate coordinate,  GeometryFactory geometryFactory) {
    double x = coordinate.getX();
    double y = coordinate.getY();
    if (x == 180D || x == -180D) {
      if (last.getX() == 180D || last.getX() == -180D) {
        return Collections.singletonList(new Coordinate(last.getX(), y, coordinate.getZ()));
      }

      double sign = Math.signum(last.getX());
      return Arrays.asList(
          last,
          new Coordinate(180D * sign, y),
          new Coordinate(180D * sign * -1, y)
      );
    }

    List<Coordinate> split;
    if ((coordinate.getX() < 0 && last.getX() > 0) || (coordinate.getX() > 0 && last.getX() < 0)) {
      LineString lineString = geometryFactory.createLineString(new Coordinate[]{last, coordinate});
      Geometry geometry = new JtsGeometry(lineString, JtsSpatialContext.GEO, true, true).getGeom();
      if (geometry instanceof LineString) {
        return Collections.singletonList(coordinate);
      } else if (geometry instanceof GeometryCollection) {
        split = geometryParse(coordinate, last, (GeometryCollection) geometry);
      } else {
        throw new IllegalStateException(
            String.format("An error occurred splitting AM, type: %s from coordinates (%f, %f, %s) to (%f, %f, %s)",
                geometry.toString(),
                coordinate.getX(), coordinate.getY(), Instant.ofEpochMilli((long) coordinate.getZ()),
                last.getX(), last.getY(), Instant.ofEpochMilli((long) last.getZ())
            ));
      }
    } else {
      split = Collections.singletonList(coordinate);
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
}

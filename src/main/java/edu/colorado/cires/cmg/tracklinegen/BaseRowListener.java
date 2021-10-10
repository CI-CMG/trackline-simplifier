package edu.colorado.cires.cmg.tracklinegen;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

public class BaseRowListener<T extends DataRow> implements RowListener<T> {

  private final long msSplit;
  private final GeometrySimplifier geometrySimplifier;
  private final Predicate<T> filterRow;
  private final GeoJsonMultiLineWriter lineWriter;
  private final int batchSize;
  private final long maxAllowedSimplifiedPoints;

  private long unsimplifiedPointCount = 0;
  private long simplifiedPointCount = 0;
  private long targetPointCount = 0;
  private final GeometryFactory geometryFactory;
  private boolean started = false;

  private List<PointState> pointBuffer;

  private static final double MIN_DISTANCE = 0.000001;


  public BaseRowListener(
      long msSplit,
      GeometrySimplifier geometrySimplifier,
      GeoJsonMultiLineWriter lineWriter,
      int batchSize,
      Predicate<T> filterRow,
      long maxAllowedSimplifiedPoints,
      GeometryFactory geometryFactory) {
    this.msSplit = msSplit;
    this.geometrySimplifier = geometrySimplifier;
    this.lineWriter = lineWriter;
    this.filterRow = filterRow;
    this.batchSize = batchSize;
    this.maxAllowedSimplifiedPoints = maxAllowedSimplifiedPoints;
    this.geometryFactory = geometryFactory;
  }

  @Override
  public void start() {
    pointBuffer = new ArrayList<>();
  }


  private boolean isDifferentFromLast(PointState thisPoint) {
    if (pointBuffer.isEmpty()) {
      return true;
    }
    PointState lastPoint = pointBuffer.get(pointBuffer.size() - 1);
    double distance = thisPoint.getPoint().distance(lastPoint.getPoint());
    return distance > MIN_DISTANCE;
  }

  private List<List<PointState>> splitBufferSegments() {
    final List<List<PointState>> segments = new ArrayList<>();
    for (int i = 0; i < pointBuffer.size(); i++) {
      PointState thisPoint = pointBuffer.get(i);
      if (i == 0) {
        List<PointState> segment = new ArrayList<>();
        segments.add(segment);
        segment.add(thisPoint);
      } else {
        List<PointState> lastSegment = segments.get(segments.size() - 1);
        PointState lastPoint = lastSegment.get(lastSegment.size() - 1);
        if (shouldSplit(lastPoint, thisPoint)) {
          List<PointState> segment = new ArrayList<>();
          thisPoint.setIndex(0);
          segment.add(thisPoint);
          segments.add(segment);
        } else {
          thisPoint.setIndex(lastPoint.getIndex() + 1);
          lastSegment.add(thisPoint);
        }
      }
    }

    return segments;
  }

  private PointState simplifySegments(List<List<PointState>> segments, boolean finish) {
    PointState nextPointState = null;
    for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {

      boolean firstSegment = segmentIndex == 0;
      boolean lastSegment = segmentIndex == segments.size() - 1;
      List<PointState> segment = segments.get(segmentIndex);

      if (segment.size() > 1) {
        LineString simplified = simplify(segment.stream().map(PointState::getPoint).map(Point::getCoordinate).collect(Collectors.toList()));
        Coordinate[] coordinates = simplified.getCoordinateSequence().toCoordinateArray();
        if (coordinates.length > 2 || (coordinates.length == 2 && distance(coordinates[0], coordinates[1]) > MIN_DISTANCE)) {
          if (segment.get(0).getIndex() == 0) {
            if (!started) {
              lineWriter.start();
              started = true;
            }
            lineWriter.startLine();
          }
          int maxIndex = coordinates.length - 1;
          if (!finish && lastSegment) {
            maxIndex = maxIndex - 1;
          }
          for (int coordinateIndex = 0; coordinateIndex <= maxIndex; coordinateIndex++) {
            Coordinate coordinate = coordinates[coordinateIndex];
            incrementSimplifiedPointCount();
            writeCoordinate(coordinate);
          }
          if (finish || !lastSegment) {
            lineWriter.endLine();
          } else {
            nextPointState = new PointState(geometryFactory.createPoint(coordinates[coordinates.length - 1]), true);
            nextPointState.setIndex(coordinates.length - 1);
          }
        } else {
          throw new IllegalStateException("Simplified LineString resulted in a point");
        }
      } else if (segment.size() == 1) {
        if (lastSegment) {
          nextPointState = segment.get(0);
        } else if (firstSegment) {
          incrementSimplifiedPointCount();
          writeCoordinate(segment.get(0).getPoint().getCoordinate());
          lineWriter.endLine();
        } else {
          throw new IllegalStateException("Singleton segment created");
        }
      } else {
        throw new IllegalStateException("Empty segment created");
      }
    }

    if (!finish && nextPointState == null) {
      throw new NullPointerException("Next point state is null");
    }

    return nextPointState;
  }

  private static List<List<PointState>> removeNonTarget(List<List<PointState>> segments) {
    for (List<PointState> segment : segments) {
      ListIterator<PointState> it = segment.listIterator();
      int removedCount = 0;
      boolean foundConfirmed = false;
      while (it.hasNext()) {
        PointState pointState = it.next();
        if (!foundConfirmed && !pointState.isConfirmed()) {
          removedCount++;
          it.remove();
        } else {
          foundConfirmed = true;
          pointState.setIndex(pointState.getIndex() - removedCount);
        }
      }
      if (!segment.isEmpty()) {
        it = segment.listIterator(segment.size());
        while (it.hasPrevious()) {
          PointState pointState = it.previous();
          if (!pointState.isConfirmed()) {
            it.remove();
          } else {
            break;
          }
        }
      }
    }
    return segments;
  }

  private static List<List<PointState>> removeSingletons(List<List<PointState>> segments, boolean finish, boolean started) {

    if (finish && segments.size() == 1 && segments.get(0).size() == 1) {
      return segments;
    }

    List<List<PointState>> filtered = new ArrayList<>();
    for (int i = 0; i < segments.size(); i++) {
      List<PointState> segment = segments.get(i);
      if (segment.size() >= 2
          || (segment.size() == 1 && i == 0 && segment.get(0).getIndex() > 0)
          || (!finish && segment.size() == 1 && i == segments.size() - 1)
      ) {
        filtered.add(segment);
      }
    }

    if (!started && finish && filtered.isEmpty() && !segments.isEmpty()) {
      List<PointState> segment = segments.stream()
          .filter(list -> !list.isEmpty())
          .findFirst()
          .orElseThrow(() -> new IllegalStateException("Empty LineString generated"));
      filtered.add(segment);
    }

    return filtered;
  }

  private void writeSimplified() {
    final List<List<PointState>> segments = removeSingletons(removeNonTarget(splitBufferSegments()), false, started);
    List<PointState> lastSegment = segments.get(segments.size() - 1);
    lastSegment.get(lastSegment.size() - 1).setConfirmed(true);
    int count = segments.stream().map(List::size).reduce(0, Integer::sum);
    if (count <= batchSize) {
      pointBuffer = segments.stream().flatMap(List::stream).collect(Collectors.toList());
    } else {
      PointState nextPointState = simplifySegments(segments, false);
      pointBuffer = new ArrayList<>();
      pointBuffer.add(nextPointState);
    }
  }

  private double distance(Coordinate coordinate1, Coordinate coordinate2) {
    return geometryFactory.createPoint(coordinate1).distance(geometryFactory.createPoint(coordinate2));
  }

  @Override
  public void processRow(T row) {
    unsimplifiedPointCount++;
    PointState pointState = new PointState(geometryFactory.createPoint(toCoordinate(row)), isDesiredRowType(row));
    if (pointState.isTarget()) {
      targetPointCount++;
    }
    if (pointBuffer.isEmpty()) {
      pointBuffer.add(pointState);
    } else if (isDifferentFromLast(pointState)) {
      pointBuffer.add(pointState);
      if (pointBuffer.size() > batchSize) {
        writeSimplified();
      }
    } else {
      PointState lastPoint = pointBuffer.get(pointBuffer.size() - 1);
      if (pointState.isTarget()) {
        lastPoint.setTarget(true);
        lastPoint.setConfirmed(true);
      }
    }
  }

  @Override
  public void finish() {

    final List<List<PointState>> segments = removeSingletons(removeNonTarget(splitBufferSegments()), true, started);
    int count = segments.stream().map(List::size).reduce(0, Integer::sum);
    if (started) {
      if (count != 0) {
        simplifySegments(segments, true);
      }
    } else {
      if (count == 0) {
        throw new IllegalStateException("Empty LineString generated");
      } else if (count == 1) {
        lineWriter.startPoint();
        started = true;
        incrementSimplifiedPointCount();
        PointState pointState = segments.get(0).get(0);
        writeCoordinate(pointState.getPoint().getCoordinate());
      } else {
        simplifySegments(segments, true);
      }
    }

    GeometryProperties properties = GeometryProperties.Builder.configure()
        .withSimplifiedPointCount(simplifiedPointCount)
        .withUnsimplifiedPointCount(unsimplifiedPointCount)
        .withTargetPointCount(targetPointCount)
        .build();

    lineWriter.finish(properties, count > 1);
  }

  private boolean isDesiredRowType(T row) {
    return filterRow.test(row);
  }

  private boolean shouldSplit(PointState point1, PointState point2) {
    return isSplittingEnabled() && point2.getPoint().getCoordinate().getZ() - point1.getPoint().getCoordinate().getZ() > msSplit;
  }

  private boolean isSplittingEnabled() {
    return msSplit > 0;
  }

  private void writeCoordinate(Coordinate coordinate) {
    List<Double> args = new ArrayList<>(4);
    args.add(coordinate.getX());
    args.add(coordinate.getY());

    if (!Double.isNaN(coordinate.getZ())) {
      args.add(coordinate.getZ());
      if (!Double.isNaN(coordinate.getM())) {
        args.add(coordinate.getM());
      }
    }
    lineWriter.writeCoordinate(args);
  }

  private void incrementSimplifiedPointCount() {
    if (maxAllowedSimplifiedPoints > 0 && simplifiedPointCount > maxAllowedSimplifiedPoints) {
      throw new SimplifiedPointCountExceededException(
          "Simplified point count exceeded: allowed = " + maxAllowedSimplifiedPoints + " batched points = " + simplifiedPointCount
      );
    }
    simplifiedPointCount++;
  }

  private Coordinate toCoordinate(T row) {
    return new Coordinate(row.getLon(), row.getLat(), row.getTimestamp().toEpochMilli());
  }

  private LineString simplify(List<Coordinate> coordinates) {
    return geometrySimplifier.simplifyGeometry(coordinates);
  }

}

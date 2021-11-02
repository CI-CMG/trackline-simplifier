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

  private final double minDistance;


  public BaseRowListener(
      long msSplit,
      GeometrySimplifier geometrySimplifier,
      GeoJsonMultiLineWriter lineWriter,
      int batchSize,
      Predicate<T> filterRow,
      long maxAllowedSimplifiedPoints,
      GeometryFactory geometryFactory,
      int geoJsonPrecision
  ) {
    this.msSplit = msSplit;
    this.geometrySimplifier = geometrySimplifier;
    this.lineWriter = lineWriter;
    this.filterRow = filterRow;
    this.batchSize = batchSize;
    this.maxAllowedSimplifiedPoints = maxAllowedSimplifiedPoints;
    this.geometryFactory = geometryFactory;
    this.minDistance = 1d / Math.pow(10d, geoJsonPrecision);
  }

  @Override
  public void start() {
    pointBuffer = new ArrayList<>();
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

  private List<PointState> simplifySegment(List<PointState> segment) {
    List<PointState> simplifiedSegment;
    if (segment.size() > 1) {
      int startIndex = segment.get(0).getIndex();
      LineString simplified = simplify(segment.stream().map(PointState::getPoint).map(Point::getCoordinate).collect(Collectors.toList()));
      Coordinate[] coordinates = simplified.getCoordinateSequence().toCoordinateArray();
      simplifiedSegment = new ArrayList<>();
      for (int i = 0; i < coordinates.length; i++) {
        if (i == 0 || coordinates.length > 2 || distance(coordinates[0], coordinates[1]) > minDistance) {
          Coordinate coordinate = coordinates[i];
          PointState pointState = new PointState(geometryFactory.createPoint(coordinate), true);
          pointState.setIndex(i + startIndex);
          simplifiedSegment.add(pointState);
        }
      }
    } else {
      simplifiedSegment = segment;
    }
    return simplifiedSegment;
  }


  private List<List<PointState>> writeSegments(List<List<PointState>> segments) {
    List<List<PointState>> nextBuffer = new ArrayList<>();
    for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
      boolean lastSegment = segmentIndex == segments.size() - 1;
      List<PointState> segment = segments.get(segmentIndex);

      if (lastSegment) {
        List<PointState> nextSegment = new ArrayList<>();
        nextSegment.add(segment.get(segment.size() - 1));
        nextBuffer.add(nextSegment);
        segment = segment.subList(0, segment.size() - 1);
      }

      if (!segment.isEmpty() && segment.get(0).getIndex() == 0) {
        if (!started) {
          lineWriter.start();
          started = true;
        }
        lineWriter.startLine();
      }

      int maxIndex = segment.size() - 1;

      for (int coordinateIndex = 0; coordinateIndex <= maxIndex; coordinateIndex++) {
        PointState pointState = segment.get(coordinateIndex);
        incrementSimplifiedPointCount();
        writeCoordinate(pointState.getPoint().getCoordinate());
        if (coordinateIndex == maxIndex && !lastSegment) {
          lineWriter.endLine();
        }
      }

    }
    return nextBuffer;
  }

  private List<List<PointState>> simplifySegments(List<List<PointState>> segments) {

    List<List<PointState>> simplified = removeSingletons(
        segments.stream().map(this::simplifySegment).collect(Collectors.toList()),
        started
    );

    int count = simplified.stream().map(List::size).reduce(0, Integer::sum);
    if (count < batchSize) {
      return simplified;
    }

    return writeSegments(segments);
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

  private static List<List<PointState>> removeSingletons(List<List<PointState>> segments, boolean started) {

    List<List<PointState>> filtered = new ArrayList<>();
    for (int i = 0; i < segments.size(); i++) {
      List<PointState> segment = segments.get(i);
      if (segment.isEmpty()) {
        continue;
      }
      int lastIndex = segment.get(segment.size() - 1).getIndex();
      if (lastIndex > 0 || (i == 0 && !started) || i == segments.size() - 1) {
        filtered.add(segment);
      }
    }

    // remove the first segment if it is a singleton and there are other non-singletons
    if (filtered.size() > 1 && filtered.get(0).size() == 1 && filtered.get(0).get(0).getIndex() == 0) {
      filtered = filtered.subList(1, filtered.size());
    }

    return filtered;
  }

  private void writeSimplified() {
    final List<List<PointState>> segments = removeSingletons(removeNonTarget(splitBufferSegments()), started);
    int count = segments.stream().map(List::size).reduce(0, Integer::sum);
    if (count <= batchSize) {
      pointBuffer = segments.stream().flatMap(List::stream).collect(Collectors.toCollection(ArrayList::new));
    } else {
      pointBuffer = simplifySegments(segments).stream().flatMap(List::stream).collect(Collectors.toCollection(ArrayList::new));
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

    pointBuffer.add(pointState);
    if (pointBuffer.size() > batchSize) {
      writeSimplified();
    }
  }

  private void writeFinalSegments(List<List<PointState>> segments) {
    List<List<PointState>> lastSingleton = writeSegments(segments);
    if(lastSingleton.get(0).get(0).getIndex() > 0) {
      incrementSimplifiedPointCount();
      writeCoordinate(lastSingleton.get(0).get(0).getPoint().getCoordinate());
      lineWriter.endLine();
    }
  }

  @Override
  public void finish() {

    final List<List<PointState>> segments = simplifySegments(removeSingletons(removeNonTarget(splitBufferSegments()), started));

    boolean lineString = false;

    if (started) {
      writeFinalSegments(segments);
      lineString = true;
    } else {
      int count = segments.stream().map(List::size).reduce(0, Integer::sum);
      if(count > 1) {
        writeFinalSegments(segments);
        lineString = true;
      } else {
        lineWriter.startPoint();
        started = true;
        incrementSimplifiedPointCount();
        PointState pointState = segments.get(0).get(0);
        writeCoordinate(pointState.getPoint().getCoordinate());
      }
    }

    GeometryProperties properties = GeometryProperties.Builder.configure()
        .withSimplifiedPointCount(simplifiedPointCount)
        .withUnsimplifiedPointCount(unsimplifiedPointCount)
        .withTargetPointCount(targetPointCount)
        .build();

    lineWriter.finish(properties, lineString);
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

package edu.colorado.cires.cmg.tracklinegen;

import static edu.colorado.cires.cmg.tracklinegen.AntimeridianUtils.getDistance;
import static edu.colorado.cires.cmg.tracklinegen.AntimeridianUtils.getSpeed;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

public class BaseRowListener<T extends DataRow> implements RowListener<T> {

  private final long NmSplit;
  private final long msSplit;
  private final GeometrySimplifier geometrySimplifier;
  private final Predicate<T> filterRow;
  private final GeoJsonMultiLineWriter lineWriter;
  private final int batchSize;
  private final long maxAllowedSimplifiedPoints;
  private final double maxAllowedSpeedKnts;

  private long unsimplifiedPointCount = 0;
  private long simplifiedPointCount = 0;
  private long targetPointCount = 0;
  private final GeometryFactory geometryFactory;
  private boolean started = false;

  private List<PointState> pointBuffer;

  private final double minDistance;
  private final DecimalFormat format;

  /**
   *
   * @param NmSplit Nautical miles value by which to split trackline segments
   * @param msSplit
   * @param geometrySimplifier
   * @param lineWriter
   * @param batchSize
   * @param filterRow
   * @param maxAllowedSimplifiedPoints
   * @param geometryFactory
   * @param geoJsonPrecision
   * @deprecated Use {@link BaseRowListener#BaseRowListener(long, long, GeometrySimplifier, GeoJsonMultiLineWriter, int, Predicate, long, GeometryFactory, int, double)}
   *
   */
  @Deprecated
  public BaseRowListener(
      long NmSplit,
      long msSplit,
      GeometrySimplifier geometrySimplifier,
      GeoJsonMultiLineWriter lineWriter,
      int batchSize,
      Predicate<T> filterRow,
      long maxAllowedSimplifiedPoints,
      GeometryFactory geometryFactory,
      int geoJsonPrecision
  ) {
    this(NmSplit, msSplit, geometrySimplifier, lineWriter, batchSize, filterRow, maxAllowedSimplifiedPoints, geometryFactory, geoJsonPrecision, 0D);
  }

  public BaseRowListener(
      long NmSplit,
      long msSplit,
      GeometrySimplifier geometrySimplifier,
      GeoJsonMultiLineWriter lineWriter,
      int batchSize,
      Predicate<T> filterRow,
      long maxAllowedSimplifiedPoints,
      GeometryFactory geometryFactory,
      int geoJsonPrecision,
      double maxAllowedSpeedKnts
  ) {

    this.NmSplit = NmSplit;
    this.msSplit = msSplit;
    this.geometrySimplifier = geometrySimplifier;
    this.lineWriter = lineWriter;
    this.filterRow = filterRow;
    this.batchSize = batchSize;
    this.maxAllowedSimplifiedPoints = maxAllowedSimplifiedPoints;
    this.geometryFactory = geometryFactory;
    this.minDistance = 1d / Math.pow(10d, geoJsonPrecision);
    StringBuilder sb = new StringBuilder("0.");
    for (int i = 1; i <= geoJsonPrecision; i++) {
      sb.append("#");
    }
    format = new DecimalFormat(sb.toString(), DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    this.maxAllowedSpeedKnts = maxAllowedSpeedKnts;
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

  private enum Sign {
    POSITIVE,
    NEGATIVE
  }

  private static Sign findPreviousSign(List<Coordinate> coordinates, int index) {
    Sign sign = null;
    for (int i = index - 1; i >= 0; i--) {
      Coordinate coordinate = coordinates.get(i);
      if(!is180(coordinate)) {
        sign = coordinate.getX() < 0D ? Sign.NEGATIVE : Sign.POSITIVE;
        break;
      }
    }
    return sign;
  }

  private static Sign findNextSign(List<Coordinate> coordinates, int index) {
    Sign sign = null;
    for (int i = index + 1; i < coordinates.size(); i++) {
      Coordinate coordinate = coordinates.get(i);
      if(!is180(coordinate)) {
        sign = coordinate.getX() < 0D ? Sign.NEGATIVE : Sign.POSITIVE;
        break;
      }
    }
    return sign;
  }

  private static boolean is180(Coordinate coordinate) {
    return Math.abs(coordinate.getX()) - 180D == 0D;
  }

  private List<Coordinate> correctSigns(List<Coordinate> coordinates) {
    List<Coordinate> corrected = new ArrayList<>(coordinates.size());
    for (int i = 0; i < coordinates.size(); i++) {
      Coordinate coordinate = coordinates.get(i);

      if(is180(coordinate)) {
        Sign sign = findPreviousSign(coordinates, i);
        if(sign == null) {
          sign = findNextSign(coordinates, i);
        } if (sign == null) {
          sign = Sign.POSITIVE;
        }
        double correctedX;
        switch (sign) {
          case NEGATIVE:
            correctedX = -180D;
            break;
          case POSITIVE:
            correctedX = 180D;
            break;
          default:
            throw new IllegalStateException("This will never happen");
        }
        double y = coordinate.getY();
        double time = coordinate.getZ();

        Coordinate c2 = new Coordinate(correctedX, y, time);

        if(i > 0) {
          Coordinate c1 = corrected.get(i - 1);
          double m = getDistance(c1, c2);
          try {
            getSpeed(maxAllowedSpeedKnts, c1, c2, m);
          } catch (ValidationException e) {
            throw new IllegalStateException("Invalid speed", e);
          }
        }


        corrected.add(c2);



      } else {
        if(i > 0) {
          Coordinate c1 = corrected.get(i - 1);
          double m = getDistance(c1, coordinate);
          try {
            getSpeed(maxAllowedSpeedKnts, c1, coordinate, m);
          } catch (ValidationException e) {
            throw new IllegalStateException("Invalid speed", e);
          }
        }

        corrected.add(coordinate);
      }

    }
    return corrected;
  }

  private LineString correctSigns(LineString lineString) {
    List<Coordinate> corrected = correctSigns(Arrays.asList(lineString.getCoordinates()));
    return geometryFactory.createLineString(corrected.toArray(new Coordinate[0]));
  }

  private List<PointState> simplifySegment(List<PointState> segment) {
    List<PointState> simplifiedSegment;
    if (segment.size() > 1) {
      int startIndex = segment.get(0).getIndex();
      LineString simplified = simplify(
          correctSigns(segment.stream().map(PointState::getPoint).map(Point::getCoordinate).collect(Collectors.toList())));
      simplified = correctSigns(simplified);
      Coordinate[] coordinates = simplified.getCoordinateSequence().toCoordinateArray();
      simplifiedSegment = new ArrayList<>();
      for (int i = 0; i < coordinates.length; i++) {
        if (i == 0 || coordinates.length > 2 || distance(coordinates[0], coordinates[1]) > minDistance) {
          Coordinate coordinate = coordinates[i];
          PointState pointState = new PointState(geometryFactory.createPoint(coordinate), true);
          pointState.setSimplified(true);
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
      boolean foundTarget = false;
      while (it.hasNext()) {
        PointState pointState = it.next();
        if (!foundTarget && !pointState.isTarget()) {
          removedCount++;
          it.remove();
        } else {
          foundTarget = true;
          pointState.setIndex(pointState.getIndex() - removedCount);
        }
      }
      if (!segment.isEmpty()) {
        it = segment.listIterator(segment.size());
        while (it.hasPrevious()) {
          PointState pointState = it.previous();
          if (!pointState.isTarget()) {
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
    if (lastSingleton.get(0).get(0).getIndex() > 0) {
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
      if (count > 1) {
        writeFinalSegments(segments);
        lineString = true;
      } else if (count == 1) {
        lineWriter.startPoint();
        started = true;
        incrementSimplifiedPointCount();
        PointState pointState = segments.get(0).get(0);
        writeCoordinate(pointState.getPoint().getCoordinate());
      } else {
        lineWriter.start();
        lineString = true;
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
    if (point2.isSimplified()) {
      return point2.getIndex() == 0;
    } if (isSplittingByMsEnabled() || isSplittingByNmEnabled()) {
      double difference = point2.getPoint().getCoordinate().getZ() - point1.getPoint().getCoordinate().getZ();
      // Calculate distance between two points by retrieving distance in meters and converting to nautical miles
      double distance = (getDistance(point1.getPoint().getCoordinate(), point2.getPoint().getCoordinate()) / 1852);
      return (difference > msSplit) || (distance > NmSplit);
    }

    return false;
  }

  private boolean isSplittingByMsEnabled() {
    return msSplit > 0;
  }

  private boolean isSplittingByNmEnabled() {
    return NmSplit > 0;
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

  private double round(double value) {
    return Double.parseDouble(format.format(value));
  }

  private Coordinate toCoordinate(T row) {
    if (row.getTimestamp() == null) {
      return new Coordinate(round(row.getLon()), round(row.getLat()));
    }

    else {
      return new Coordinate(round(row.getLon()), round(row.getLat()), row.getTimestamp().toEpochMilli());
    }
  }

  private LineString simplify(List<Coordinate> coordinates) {
    return geometrySimplifier.simplifyGeometry(coordinates);
  }

}

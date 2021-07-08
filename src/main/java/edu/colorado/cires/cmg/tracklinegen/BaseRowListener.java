package edu.colorado.cires.cmg.tracklinegen;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

public class BaseRowListener<T extends DataRow> implements RowListener<T> {

  private final long msSplit;
  private final GeometrySimplifier geometrySimplifier;
  private final Predicate<T> filterRow;
  private final GeoJsonMultiLineWriter lineWriter;
  private final int batchSize;
  private final long maxAllowedSimplifiedPoints;

  private List<Coordinate> confirmedLineString;
  private List<Coordinate> potentialLineString;
  private Long lastTimestamp;
  private boolean writingLine = false;
  private long unsimplifiedPointCount = 0;
  private long simplifiedPointCount = 0;
  private long targetPointCount = 0;


  public BaseRowListener(
      long msSplit,
      GeometrySimplifier geometrySimplifier,
      GeoJsonMultiLineWriter lineWriter,
      int batchSize,
      Predicate<T> filterRow,
      long maxAllowedSimplifiedPoints) {
    this.msSplit = msSplit;
    this.geometrySimplifier = geometrySimplifier;
    this.lineWriter = lineWriter;
    this.filterRow = filterRow;
    this.batchSize = batchSize;
    this.maxAllowedSimplifiedPoints = maxAllowedSimplifiedPoints;
  }

  public BaseRowListener(
      long msSplit,
      GeometrySimplifier geometrySimplifier,
      GeoJsonMultiLineWriter lineWriter,
      int batchSize,
      Predicate<T> filterRow) {
    this(msSplit, geometrySimplifier, lineWriter, batchSize, filterRow, 0);
  }

  @Override
  public void start() {
    confirmedLineString = new LinkedList<>();
    potentialLineString = new LinkedList<>();
    lineWriter.start();
  }

  @Override
  public void processRow(T row) {
    long timestamp = row.getTimestamp().toEpochMilli();
    if (isBatchFull()) {
      writeConfirmed();
    }
    if (shouldSplit(timestamp)) {
      split();
    }
    if (isDesiredRowType(row)) {
      addConfirmedRow(row);
    } else {
      addPotentialRow(row);
    }
  }

  @Override
  public void finish() {
    if (writingLine) {
      if (confirmedLineString.size() >= 2) {
        writeConfirmed();
      } else if (confirmedLineString.size() == 1) {
        incrementSimplifiedPointCount();
        unsimplifiedPointCount++;
        writeCoordinate(confirmedLineString.get(0));
      }
      endLine();
    } else {
      split();
    }

    GeometryProperties properties = GeometryProperties.Builder.configure()
        .withSimplifiedPointCount(simplifiedPointCount)
        .withUnsimplifiedPointCount(unsimplifiedPointCount)
        .withTargetPointCount(targetPointCount)
        .build();

    lineWriter.finish(properties);
  }

  private boolean isDesiredRowType(T row) {
    return filterRow.test(row);
  }

  private boolean isBatchFull() {
    return confirmedLineString.size() > batchSize;
  }

  private boolean shouldSplit(long timestamp) {
    return isSplittingEnabled() && lastTimestamp != null && timestamp - lastTimestamp > msSplit;
  }

  private boolean isSplittingEnabled() {
    return msSplit > 0;
  }

  private void maybeStartLine() {
    if (!writingLine) {
      lineWriter.startLine();
      writingLine = true;
    }
  }

  private void maybeEndLine() {
    if (writingLine) {
      lineWriter.endLine();
      writingLine = false;
    }
  }

  private void endLine() {
    lineWriter.endLine();
    writingLine = false;
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
    simplifiedPointCount++;
    if (maxAllowedSimplifiedPoints > 0 && simplifiedPointCount > maxAllowedSimplifiedPoints) {
      throw new SimplifiedPointCountExceededException(
          "Simplified point count exceeded: allowed = " + maxAllowedSimplifiedPoints + " batched points = " + simplifiedPointCount
      );
    }
  }

  private void writeConfirmed() {
    unsimplifiedPointCount += confirmedLineString.size();
    LineString simplified = simplify(confirmedLineString);
    maybeStartLine();
    for (Coordinate coordinate : simplified.getCoordinateSequence().toCoordinateArray()) {
      incrementSimplifiedPointCount();
      writeCoordinate(coordinate);
    }
    confirmedLineString = new LinkedList<>();
  }

  private void split() {

    maybeEndLine();

    if (confirmedLineString.size() >= 2) {
      writeConfirmed();
      endLine();
    } else if (confirmedLineString.size() == 1) {
      confirmedLineString = new LinkedList<>();
      unsimplifiedPointCount++;
    }

    potentialLineString = new LinkedList<>();
  }

  private Coordinate toCoordinate(T row) {
    return new Coordinate(row.getLon(), row.getLat(), row.getTimestamp().toEpochMilli());
  }

  private void addConfirmedRow(T row) {
    targetPointCount++;
    lastTimestamp = row.getTimestamp().toEpochMilli();
    confirmedLineString.addAll(potentialLineString);
    confirmedLineString.add(toCoordinate(row));
    potentialLineString = new LinkedList<>();
  }

  private void addPotentialRow(T row) {
    if (lastTimestamp != null) {
      potentialLineString.add(toCoordinate(row));
    }
  }

  private LineString simplify(List<Coordinate> coordinates) {
    return geometrySimplifier.simplifyGeometry(coordinates);
  }


}

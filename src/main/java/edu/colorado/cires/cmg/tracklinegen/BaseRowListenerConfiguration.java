package edu.colorado.cires.cmg.tracklinegen;

import java.util.function.Predicate;
import org.locationtech.jts.geom.GeometryFactory;

public class BaseRowListenerConfiguration<T extends DataRow> {

  private final long nmSplit;
  private final long msSplit;
  private final GeometrySimplifier geometrySimplifier;
  private final Predicate<T> filterRow;
  private final GeoJsonMultiLineWriter lineWriter;
  private final int batchSize;
  private final long maxAllowedSimplifiedPoints;
  private final double maxAllowedSpeedKnts;
  private final GeometryFactory geometryFactory;
  private final int geoJsonPrecision;

  private BaseRowListenerConfiguration(Builder<T> builder) {

    nmSplit = builder.nmSplit;
    msSplit = builder.msSplit;
    geometrySimplifier = builder.geometrySimplifier;
    filterRow = builder.filterRow;
    lineWriter = builder.lineWriter;
    batchSize = builder.batchSize;
    maxAllowedSimplifiedPoints = builder.maxAllowedSimplifiedPoints;
    maxAllowedSpeedKnts = builder.maxAllowedSpeedKnts;
    geometryFactory = builder.geometryFactory;
    geoJsonPrecision = builder.geoJsonPrecision;

  }

  public long getNmSplit() {
    return nmSplit;
  }

  public long getMsSplit() {
    return msSplit;
  }

  public GeometrySimplifier getGeometrySimplifier() {
    return geometrySimplifier;
  }

  public Predicate<T> getFilterRow() {
    return filterRow;
  }

  public GeoJsonMultiLineWriter getLineWriter() {
    return lineWriter;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public long getMaxAllowedSimplifiedPoints() {
    return maxAllowedSimplifiedPoints;
  }

  public double getMaxAllowedSpeedKnts() {
    return maxAllowedSpeedKnts;
  }

  public GeometryFactory getGeometryFactory() {
    return geometryFactory;
  }

  public int getGeoJsonPrecision() {
    return geoJsonPrecision;
  }

  public static <T extends DataRow> Builder<T> configure() {
    return new Builder<>();
  }

  public static <T extends DataRow> Builder<T> configure(BaseRowListenerConfiguration<T> properties) {
    return new Builder<>(properties);
  }

  public static class Builder<T extends DataRow> {

    private long nmSplit;
    private long msSplit;
    private GeometrySimplifier geometrySimplifier;
    private Predicate<T> filterRow;
    private GeoJsonMultiLineWriter lineWriter;
    private int batchSize;
    private long maxAllowedSimplifiedPoints;
    private double maxAllowedSpeedKnts;
    private GeometryFactory geometryFactory;
    private int geoJsonPrecision;

    private Builder() {

    }

    private Builder(BaseRowListenerConfiguration<T> properties) {
      nmSplit = properties.nmSplit;
      msSplit = properties.msSplit;
      geometrySimplifier = properties.geometrySimplifier;
      filterRow = properties.filterRow;
      lineWriter = properties.lineWriter;
      batchSize = properties.batchSize;
      maxAllowedSimplifiedPoints = properties.maxAllowedSimplifiedPoints;
      maxAllowedSpeedKnts = properties.maxAllowedSpeedKnts;
      geometryFactory = properties.geometryFactory;
      geoJsonPrecision = properties.geoJsonPrecision;
    }

    public Builder<T> withNmSplit(Long nmSplit) {
      this.nmSplit = nmSplit;
      return this;
    }

    public Builder<T> withMsSplit(Long msSplit) {
      this.msSplit = msSplit;
      return this;
    }

    public Builder<T> withGeometrySimplifier(GeometrySimplifier geometrySimplifier) {
      this.geometrySimplifier = geometrySimplifier;
      return this;
    }

    public Builder<T> withFilterRow(Predicate<T> filterRow) {
      this.filterRow = filterRow;
      return this;
    }

    public Builder<T> withLineWriter(GeoJsonMultiLineWriter lineWriter) {
      this.lineWriter = lineWriter;
      return this;
    }

    public Builder<T> withBatchSize(Integer batchSize) {
      this.batchSize = batchSize;

      return this;
    }

    public Builder<T> withMaxAllowedSimplifiedPoints(Long maxAllowedSimplifiedPoints) {
      this.maxAllowedSimplifiedPoints = maxAllowedSimplifiedPoints;

      return this;
    }

    public Builder<T> withMaxAllowedSpeedKnts(Double maxAllowedSpeedKnts) {
      this.maxAllowedSpeedKnts = maxAllowedSpeedKnts;
      return this;
    }

    public Builder<T> withGeometryFactory(GeometryFactory geometryFactory) {
      this.geometryFactory = geometryFactory;
      return this;
    }

    public Builder<T> withGeoJsonPrecision(int geoJsonPrecision) {
      this.geoJsonPrecision = geoJsonPrecision;
      return this;
    }

    public BaseRowListenerConfiguration<T> build() {
      return new BaseRowListenerConfiguration<>(this);
    }
  }
}



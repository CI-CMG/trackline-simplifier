package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import edu.colorado.cires.cmg.tracklinegen.BaseRowListener;
import edu.colorado.cires.cmg.tracklinegen.BaseRowListenerConfiguration;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineWriter;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import java.util.function.Predicate;
import org.locationtech.jts.geom.GeometryFactory;

public class GsBaseRowListener extends BaseRowListener<GeoDataRow> {

  public GsBaseRowListener(long nmSplit, long msSplit, GeometrySimplifier geometrySimplifier,
      GeoJsonMultiLineWriter lineWriter, int batchSize, long maxCount,
      GeometryFactory geometryFactory, int geoJsonPrecision, Predicate<GeoDataRow> rowFilter, double maxAllowedSpeedKnts) {

    super(BaseRowListenerConfiguration.<GeoDataRow>configure()
        .withNmSplit(nmSplit)
        .withMsSplit(msSplit)
        .withGeometrySimplifier(geometrySimplifier)
        .withLineWriter(lineWriter)
        .withBatchSize(batchSize)
        .withFilterRow(rowFilter)
        .withMaxAllowedSimplifiedPoints(maxCount)
        .withGeometryFactory(geometryFactory)
        .withGeoJsonPrecision(geoJsonPrecision)
        .withMaxAllowedSpeedKnts(maxAllowedSpeedKnts)
        .build());
  }
}

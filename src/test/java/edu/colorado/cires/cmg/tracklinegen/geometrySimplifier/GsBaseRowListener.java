package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import edu.colorado.cires.cmg.tracklinegen.BaseRowListener;
import edu.colorado.cires.cmg.tracklinegen.DataRow;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineWriter;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import java.util.function.Predicate;
import org.locationtech.jts.geom.GeometryFactory;

public class GsBaseRowListener extends BaseRowListener<DataRow> {

  public GsBaseRowListener(long msSplit, GeometrySimplifier geometrySimplifier,
      GeoJsonMultiLineWriter lineWriter, int batchSize, long maxCount,
      GeometryFactory geometryFactory, int geoJsonPrecision, Predicate<DataRow> rowFilter) {
    super(msSplit,
        geometrySimplifier,
        lineWriter,
        batchSize,
        rowFilter,
        maxCount,
        geometryFactory,
        geoJsonPrecision);
  }
}

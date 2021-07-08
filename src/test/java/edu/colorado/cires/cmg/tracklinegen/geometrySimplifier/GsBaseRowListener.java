package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import edu.colorado.cires.cmg.tracklinegen.BaseRowListener;
import edu.colorado.cires.cmg.tracklinegen.DataRow;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineWriter;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;

public class GsBaseRowListener extends BaseRowListener<DataRow> {

  public GsBaseRowListener(long msSplit, GeometrySimplifier geometrySimplifier,
      GeoJsonMultiLineWriter lineWriter, int batchSize, long maxCount) {
    super(msSplit, geometrySimplifier, lineWriter, batchSize, dataRow -> true, maxCount);
  }
}

package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import edu.colorado.cires.cmg.tracklinegen.RowProcessor;
import java.util.List;

public class GsRowProcessor extends RowProcessor<GeoDataRow, GsBaseRowListener> {

  public GsRowProcessor(List<GsBaseRowListener> listeners) {
    super(listeners);
  }
}

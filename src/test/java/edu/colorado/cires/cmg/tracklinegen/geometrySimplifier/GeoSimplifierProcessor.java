package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.colorado.cires.cmg.tracklinegen.DataRow;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import edu.colorado.cires.cmg.tracklinegen.TracklineProcessor;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import org.locationtech.jts.geom.GeometryFactory;

public class GeoSimplifierProcessor extends TracklineProcessor<GeoSimplifierContext, DataRow, GsBaseRowListener> {
  private int geoJsonPrecision = 5;
  private long NmSplit;
  private long msSplit;
  private GeometrySimplifier geometrySimplifier;
  private int simplifierBatchSize;
  private Path fnvFile;
  private ObjectMapper objectMapper;
  private Path gsf;
  private long maxCount;
  private final GeometryFactory geometryFactory;
  private final Predicate<GeoDataRow> rowFilter;
  private final double maxAllowedSpeedKnts;

  public GeoSimplifierProcessor(int geoJsonPrecision, long NmSplit, long msSplit, GeometrySimplifier geometrySimplifier, int simplifierBatchSize,
      Path fnvFile, ObjectMapper objectMapper, Path gsf, long maxCount, GeometryFactory geometryFactory,
      Predicate<GeoDataRow> rowFilter, double maxAllowedSpeedKnts) {
    this.geoJsonPrecision = geoJsonPrecision;
    this.NmSplit = NmSplit;
    this.msSplit = msSplit;
    this.geometrySimplifier = geometrySimplifier;
    this.simplifierBatchSize = simplifierBatchSize;
    this.fnvFile = fnvFile;
    this.objectMapper = objectMapper;
    this.gsf = gsf;
    this.maxCount = maxCount;
    this.geometryFactory = geometryFactory;
    this.rowFilter = rowFilter;
    this.maxAllowedSpeedKnts = maxAllowedSpeedKnts;
  }

  @Override
  protected Iterator<DataRow> getRows(GeoSimplifierContext context) {
    return new BufferReaderIterator(context.getReader());
  }

  @Override
  protected List<GsBaseRowListener> createRowListeners(GeoSimplifierContext context) {
    return Collections.singletonList(new GsBaseRowListener(
        NmSplit,
        msSplit,
        geometrySimplifier,
        context.getLineWriter(),
        simplifierBatchSize,
        maxCount,
        geometryFactory,
        geoJsonPrecision,
        rowFilter,
        maxAllowedSpeedKnts
        )
    );
  }

  @Override
  protected GeoSimplifierContext createProcessingContext() {
    return new GeoSimplifierContext(fnvFile, geoJsonPrecision, objectMapper, gsf);
  }
}

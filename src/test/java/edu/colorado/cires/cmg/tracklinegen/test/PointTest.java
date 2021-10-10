package edu.colorado.cires.cmg.tracklinegen.test;

import static junit.framework.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineProcessor;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.GeoSimplifierProcessor;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public class PointTest {

  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

  @Test
  public void testSimplifiedPoint() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.0001;
    final int simplifierBatchSize = 3000;
    final int msSplit = 3600000;
    final long maxCount = 10000;
    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
    ObjectMapper objectMapper = new ObjectMapper();
    Path dataFile = Paths.get("src/test/resources/single-point-test.txt");

    Path gsf = Paths.get("target/single-point-test.p1");

    double maxAllowedSpeedKnts = 0D;

    byte[] geoJsonBytes = null;
    byte[] wktBytes = null;

    GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
        geoJsonPrecision, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory
    );
    phase1.process();


    final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

    try(InputStream in = Files.newInputStream(gsf)) {
      GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
          objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
      );
      phase2.process(in, geoJsonOut, wktOut);
    }

    geoJsonBytes = geoJsonOut.toByteArray();
    wktBytes = wktOut.toByteArray();

    assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/single-point-test.json"))), new String(geoJsonBytes));

    assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/single-point-test.wkt"))), new String(wktBytes));

  }

}

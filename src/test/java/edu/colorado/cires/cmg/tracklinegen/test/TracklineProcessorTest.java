package edu.colorado.cires.cmg.tracklinegen.test;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import edu.colorado.cires.cmg.tracklinegen.GeometryProperties.Builder;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.GeoSimplifierProcessor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TracklineProcessorTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void test() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.0001;
    final int simplifierBatchSize = 3000;
    final int msSplit = 3600000;
    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(0.0001);
    ObjectMapper objectMapper = new ObjectMapper();

    String actualDir = "target/test-classes/phase1/actual";
    File directory= new File(actualDir);
    if (! directory.exists()){
      directory.mkdir();
    }
    Path dataFile = Paths.get("src/test/resources/phase1/test1/data.txt");
    String gsf = actualDir + "/geoSimplfied.json";

    GeoSimplifierProcessor tracklineProcessor = new GeoSimplifierProcessor(geoJsonPrecision, msSplit, geometrySimplifier, simplifierBatchSize,
        dataFile, objectMapper, Paths.get(gsf));

    tracklineProcessor.process();

    JsonNode actual = objectMapper.readTree(new File(gsf));
    JsonNode expected = objectMapper.readTree(new File("src/test/resources/phase1/test1/expected.geojson"));
    assertEquals(expected,actual);
  }
}
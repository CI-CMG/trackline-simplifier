package edu.colorado.cires.cmg.tracklinegen.test;

import static edu.colorado.cires.cmg.tracklinegen.JsonPropertiesUtils.assertJsonEquivalent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.colorado.cires.cmg.tracklinegen.ValidationRuntimeException;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.GeoJsonProcessor;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.jackson.ObjectMapperCreator;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class Phase2GeometryTest {

  private final ObjectMapper objectMapper = ObjectMapperCreator.create();

  @Test
  public void testPointOnAm() throws Exception {
    String baseDir = "target/test-classes/phase2/testPointOnAm/";
    String actualDir = baseDir + "actual";
    File directory = new File(actualDir);
    if (!directory.exists()) {
      directory.mkdir();
    }

    Path gsf = Paths.get(baseDir + "geometry/all.geojson");
    String geoJsonFile = actualDir + "/all.geojson";
    String wktFile = actualDir + "/all.wkt";
    int geoJsonPrecision = 5;
    double maxAllowedSpeedKnts = 0D;

    GeoJsonProcessor geoJsonProcessor = new GeoJsonProcessor(gsf, Paths.get(geoJsonFile), Paths.get(wktFile),
        objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);
    geoJsonProcessor.process();

    String expectedDir = baseDir + "expected";
    JsonNode expected = objectMapper.readTree(new File(expectedDir + "/all.geojson"));
    JsonNode actual = objectMapper.readTree(new File(geoJsonFile));
    assertJsonEquivalent(expected, actual, 0.00001);

    BufferedReader wktExpected = Files.newBufferedReader(Paths.get(expectedDir + "/all.wkt"));
    BufferedReader wktActual = Files.newBufferedReader(Paths.get(wktFile));
    assertEquals(wktExpected.readLine(), wktActual.readLine());
  }

  @Test
  public void testMultiPointOnAm() throws Exception {
    String baseDir = "target/test-classes/phase2/testMultiPointOnAm/";
    String actualDir = baseDir + "actual";
    File directory = new File(actualDir);
    if (!directory.exists()) {
      directory.mkdir();
    }

    Path gsf = Paths.get(baseDir + "geometry/all.geojson");
    String geoJsonFile = actualDir + "/all.geojson";
    String wktFile = actualDir + "/all.wkt";
    int geoJsonPrecision = 5;
    double maxAllowedSpeedKnts = 0D;

    GeoJsonProcessor geoJsonProcessor = new GeoJsonProcessor(gsf, Paths.get(geoJsonFile), Paths.get(wktFile),
        objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);
    geoJsonProcessor.process();

    String expectedDir = baseDir + "expected";
    JsonNode expected = objectMapper.readTree(new File(expectedDir + "/all.geojson"));
    JsonNode actual = objectMapper.readTree(new File(geoJsonFile));
    assertJsonEquivalent(expected, actual, 0.00001);

    BufferedReader wktExpected = Files.newBufferedReader(Paths.get(expectedDir + "/all.wkt"));
    BufferedReader wktActual = Files.newBufferedReader(Paths.get(wktFile));
    assertEquals(wktExpected.readLine(), wktActual.readLine());
  }

  @Test
  public void testCrossing() throws Exception {
    String baseDir = "target/test-classes/phase2/testBoxCrossing/";
    String actualDir = baseDir + "actual";
    File directory = new File(actualDir);
    if (!directory.exists()) {
      directory.mkdir();
    }

    Path gsf = Paths.get(baseDir + "geometry/all.geojson");
    String geoJsonFile = actualDir + "/all.geojson";
    String wktFile = actualDir + "/all.wkt";
    int geoJsonPrecision = 5;
    double maxAllowedSpeedKnts = 0D;

    GeoJsonProcessor geoJsonProcessor = new GeoJsonProcessor(gsf, Paths.get(geoJsonFile), Paths.get(wktFile),
        objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);
    geoJsonProcessor.process();

    String expectedDir = baseDir + "expected";
    JsonNode expected = objectMapper.readTree(new File(expectedDir + "/all.geojson"));
    JsonNode actual = objectMapper.readTree(new File(geoJsonFile));
    assertJsonEquivalent(expected, actual, 0.00001);

    BufferedReader wktExpected = Files.newBufferedReader(Paths.get(expectedDir + "/all.wkt"));
    BufferedReader wktActual = Files.newBufferedReader(Paths.get(wktFile));
    assertEquals(wktExpected.readLine(), wktActual.readLine());
  }

  @Test
  public void testNotCrossing() throws Exception {
    String baseDir = "target/test-classes/phase2/testBoxNotCrossing/";
    String actualDir = baseDir + "actual";
    File directory = new File(actualDir);
    if (!directory.exists()) {
      directory.mkdir();
    }

    Path gsf = Paths.get(baseDir + "geometry/all.geojson");
    String geoJsonFile = actualDir + "/all.geojson";
    String wktFile = actualDir + "/all.wkt";
    int geoJsonPrecision = 5;
    double maxAllowedSpeedKnts = 0D;

    GeoJsonProcessor geoJsonProcessor = new GeoJsonProcessor(gsf, Paths.get(geoJsonFile), Paths.get(wktFile),
        objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);
    geoJsonProcessor.process();

    String expectedDir = baseDir + "expected";
    JsonNode expected = objectMapper.readTree(new File(expectedDir + "/all.geojson"));
    JsonNode actual = objectMapper.readTree(new File(geoJsonFile));
    assertJsonEquivalent(expected, actual, 0.00001);

    BufferedReader wktExpected = Files.newBufferedReader(Paths.get(expectedDir + "/all.wkt"));
    BufferedReader wktActual = Files.newBufferedReader(Paths.get(wktFile));
    assertEquals(wktExpected.readLine(), wktActual.readLine());
  }

  @Test
  public void testOutofOrder() throws Exception {
    String baseDir = "target/test-classes/phase2/test1/";
    String actualDir = baseDir + "actual";
    File directory = new File(actualDir);
    if (!directory.exists()) {
      directory.mkdir();
    }

    Path gsf = Paths.get(baseDir + "geometry/all.geojson");
    String geoJsonFile = actualDir + "/all.geojson";
    String wktFile = actualDir + "/all.wkt";
    int geoJsonPrecision = 5;
    double maxAllowedSpeedKnts = 0D;

    GeoJsonProcessor geoJsonProcessor = new GeoJsonProcessor(gsf, Paths.get(geoJsonFile), Paths.get(wktFile),
        objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);
    geoJsonProcessor.process();

    String expectedDir = baseDir + "expected";
    JsonNode expected = objectMapper.readTree(new File(expectedDir + "/all.geojson"));
    JsonNode actual = objectMapper.readTree(new File(geoJsonFile));
    assertJsonEquivalent(expected, actual, 0.00001);

    BufferedReader wktExpected = Files.newBufferedReader(Paths.get(expectedDir + "/all.wkt"));
    BufferedReader wktActual = Files.newBufferedReader(Paths.get(wktFile));
    assertEquals(wktExpected.readLine(), wktActual.readLine());
  }

  @Test
  public void testMaxSpeed() {
    String baseDir = "target/test-classes/phase2/test1/";
    String actualDir = baseDir + "actual";
    File directory = new File(actualDir);
    if (!directory.exists()) {
      directory.mkdir();
    }

    Path gsf = Paths.get(baseDir + "geometry/all.geojson");
    String geoJsonFile = actualDir + "/all.geojson";
    String wktFile = actualDir + "/all.wkt";
    int geoJsonPrecision = 5;
    double maxAllowedSpeedKnts = 11.447974D;
    GeoJsonProcessor geoJsonProcessor = new GeoJsonProcessor(gsf, Paths.get(geoJsonFile), Paths.get(wktFile),
        objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);

    ValidationRuntimeException exception = assertThrows(ValidationRuntimeException.class, () -> {
      geoJsonProcessor.process();
    });

    assertEquals(
        "Speed from (-157.892060, 21.271820, 1970-04-22T20:39:00Z) to (-157.916370, 21.142970, 1970-04-22T21:20:00Z) was 11.447975 knots, which exceeded allowed maximum of 11.447974 knots",
        exception.getMessage());

  }

  @Test
  public void testNullDatetime() throws Exception {
    String baseDir = "target/test-classes/phase2/testNullDatetime/";
    String actualDir = baseDir + "actual";
    File directory = new File(actualDir);
    if (!directory.exists()) {
      directory.mkdir();
    }

    Path gsf = Paths.get(baseDir + "geometry/all.geojson");
    String geoJsonFile = actualDir + "/all.geojson";
    String wktFile = actualDir + "/all.wkt";
    int geoJsonPrecision = 5;
    double maxAllowedSpeedKnts = 0D;

    GeoJsonProcessor geoJsonProcessor = new GeoJsonProcessor(gsf, Paths.get(geoJsonFile), Paths.get(wktFile),
            objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);
    geoJsonProcessor.process();

    String expectedDir = baseDir + "expected";
    JsonNode expected = objectMapper.readTree(new File(expectedDir + "/all.geojson"));
    JsonNode actual = objectMapper.readTree(new File(geoJsonFile));
    assertJsonEquivalent(expected, actual, 0.00001);

    BufferedReader wktExpected = Files.newBufferedReader(Paths.get(expectedDir + "/all.wkt"));
    BufferedReader wktActual = Files.newBufferedReader(Paths.get(wktFile));
    assertEquals(wktExpected.readLine(), wktActual.readLine());
  }
}
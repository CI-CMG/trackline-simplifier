package edu.colorado.cires.cmg.tracklinegen.test;

import static edu.colorado.cires.cmg.tracklinegen.JsonPropertiesUtils.assertJsonEquivalent;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineProcessor;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.GeoSimplifierProcessor;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public class PointTest {

  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);


  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

//  @Test
//  public void ddd() throws Exception {
//    Path jsonFile = Paths.get("src/test/resources/20200430_bd7ca68151459c38e9dff2fd65ec5efe_geojson.json");
//    JsonNode json = OBJECT_MAPPER.readTree(jsonFile.toFile());
//    ArrayNode features = (ArrayNode) json.get("features");
//    try(BufferedWriter writer = Files.newBufferedWriter(Paths.get("src/test/resources/single-point4.txt"))) {
//      for (JsonNode feature : features) {
//        JsonNode geometry = feature.get("geometry");
//        JsonNode properties = feature.get("properties");
//        ArrayNode coordinates = (ArrayNode) geometry.get("coordinates");
//        double lon = coordinates.get(0).asDouble();
//        double lat = coordinates.get(1).asDouble();
//        LocalDateTime time = LocalDateTime.ofInstant(Instant.parse(properties.get("time").asText()), ZoneId.of("UTC"));
//        writer.write(String.format("%04d %02d %02d %02d %02d %09.6f,%.6f,%.6f",
//            time.get(ChronoField.YEAR),
//            time.get(ChronoField.MONTH_OF_YEAR),
//            time.get(ChronoField.DAY_OF_MONTH),
//            time.get(ChronoField.HOUR_OF_DAY),
//            time.get(ChronoField.MINUTE_OF_HOUR),
//            (double)time.get(ChronoField.SECOND_OF_MINUTE) + (double) time.get(ChronoField.MILLI_OF_SECOND) / 1000d,
//            lon,
//            lat
//            ));
//        writer.newLine();
//      }
//    }
//  }

//    @Test
//  public void ddd2() throws Exception {
//    Path xyzFile = Paths.get("src/test/resources/data.xyz");
//    try(
//        BufferedReader reader = Files.newBufferedReader(xyzFile);
//        BufferedWriter writer = Files.newBufferedWriter(Paths.get("src/test/resources/data.txt"))) {
//      String line;
//      int lineNum = 0;
//      while ((line = reader.readLine()) != null) {
//        if(lineNum > 0) {
//          line = line.trim();
//          if(!line.isEmpty()) {
//            String[] parts = line.split(",");
//            double lon = Double.parseDouble(parts[0]);
//            double lat = Double.parseDouble(parts[1]);
//            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(parts[3])), ZoneId.of("UTC"));
//            writer.write(String.format("%04d %02d %02d %02d %02d %09.6f,%.6f,%.6f",
//                time.get(ChronoField.YEAR),
//                time.get(ChronoField.MONTH_OF_YEAR),
//                time.get(ChronoField.DAY_OF_MONTH),
//                time.get(ChronoField.HOUR_OF_DAY),
//                time.get(ChronoField.MINUTE_OF_HOUR),
//                (double)time.get(ChronoField.SECOND_OF_MINUTE) + (double) time.get(ChronoField.MILLI_OF_SECOND) / 1000d,
//                lon,
//                lat
//            ));
//          }
//          writer.newLine();
//        }
//        lineNum++;
//      }
//    }
//  }

  private String convertToIso(String orig) {
    //20191011T055216Z -> 2007-12-03T10:15:30.00Z
    StringBuilder sb = new StringBuilder();
    sb.append(orig.substring(0, 4))
        .append("-")
        .append(orig.substring(4, 6))
        .append("-")
        .append(orig.substring(6, 11))
        .append(":")
        .append(orig.substring(11, 13))
        .append(":")
        .append(orig.substring(13, 15))
        .append(".000Z");
    return sb.toString();
  }

//      @Test
//  public void ddd3() throws Exception {
//    Path xyzFile = Paths.get("src/test/resources/20191214_fbf5718ac3b53952fd99064b7203f08f_pointData.xyz");
//    try(
//        BufferedReader reader = Files.newBufferedReader(xyzFile);
//        BufferedWriter writer = Files.newBufferedWriter(Paths.get("src/test/resources/single-point3.txt"))) {
//      String line;
//      int lineNum = 0;
//      while ((line = reader.readLine()) != null) {
//        if(lineNum > 0) {
//          line = line.trim();
//          if(!line.isEmpty()) {
//            String[] parts = line.split(",");
//            double lat = Double.parseDouble(parts[0]);
//            double lon = Double.parseDouble(parts[1]);
//
//            LocalDateTime time = LocalDateTime.ofInstant(Instant.parse(convertToIso(parts[3])), ZoneId.of("UTC"));
//            writer.write(String.format("%04d %02d %02d %02d %02d %09.6f,%.6f,%.6f",
//                time.get(ChronoField.YEAR),
//                time.get(ChronoField.MONTH_OF_YEAR),
//                time.get(ChronoField.DAY_OF_MONTH),
//                time.get(ChronoField.HOUR_OF_DAY),
//                time.get(ChronoField.MINUTE_OF_HOUR),
//                (double)time.get(ChronoField.SECOND_OF_MINUTE) + (double) time.get(ChronoField.MILLI_OF_SECOND) / 1000d,
//                lon,
//                lat
//            ));
//          }
//          writer.newLine();
//        }
//        lineNum++;
//      }
//    }
//  }

  @Test
  public void testSimplifiedPoint() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.0001;
    final int simplifierBatchSize = 3000;
    final int NmSplit = 20;
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
        geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
        row -> true, maxAllowedSpeedKnts);
    phase1.process();

    final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

    try (InputStream in = Files.newInputStream(gsf)) {
      GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
          objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
      );
      phase2.process(in, geoJsonOut, wktOut);
    }

    geoJsonBytes = geoJsonOut.toByteArray();
    wktBytes = wktOut.toByteArray();

    assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/single-point-test.json")), objectMapper.readTree(geoJsonBytes), 0.00001);

    assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/single-point-test.wkt"))), new String(wktBytes));

  }


  @Test
  public void testSimplifiedPoint2() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.0001;
    final int simplifierBatchSize = 3000;
    final int NmSplit = 20;
    final int msSplit = 3600000;
    final long maxCount = 10000;
    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
    ObjectMapper objectMapper = new ObjectMapper();
    Path dataFile = Paths.get("src/test/resources/single-point-test2.txt");

    Path gsf = Paths.get("target/single-point-test2.p1");

    double maxAllowedSpeedKnts = 0D;

    byte[] geoJsonBytes = null;
    byte[] wktBytes = null;

    GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
        geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
        row -> true, maxAllowedSpeedKnts);
    phase1.process();

    final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

    try (InputStream in = Files.newInputStream(gsf)) {
      GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
          objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
      );
      phase2.process(in, geoJsonOut, wktOut);
    }

    geoJsonBytes = geoJsonOut.toByteArray();
    wktBytes = wktOut.toByteArray();

    assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/single-point-test2.json")), objectMapper.readTree(geoJsonBytes), 0.00001);

    assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/single-point-test2.wkt"))), new String(wktBytes));

  }


  @Test
  public void testBadSplit() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.00001;
    final int simplifierBatchSize = 3;
    final int NmSplit = 20;
    final int msSplit = 10000;
    final long maxCount = 10000;
    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
    ObjectMapper objectMapper = new ObjectMapper();
    Path dataFile = Paths.get("src/test/resources/bad-splitting2.txt");

    Path gsf = Paths.get("target/bad-splitting2.p1");

    double maxAllowedSpeedKnts = 0D;

    byte[] geoJsonBytes = null;
    byte[] wktBytes = null;

    GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
        geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
        row -> true, maxAllowedSpeedKnts);
    phase1.process();

    final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

    try (InputStream in = Files.newInputStream(gsf)) {
      GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
          objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
      );
      phase2.process(in, geoJsonOut, wktOut);
    }

    geoJsonBytes = geoJsonOut.toByteArray();
    wktBytes = wktOut.toByteArray();

    assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/bad-splitting2.json")), objectMapper.readTree(geoJsonBytes), 0.00001);

    assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/bad-splitting2.wkt"))), new String(wktBytes));

  }

  @Test
  public void testMultiBuffer() throws Exception {
    for (int simplifierBatchSize = 2; simplifierBatchSize <= 13; simplifierBatchSize++) {
      final int geoJsonPrecision = 5;
      final double simplificationTolerance = 0.000001;
      final int NmSplit = 20;
      final int msSplit = 100000;
      final long maxCount = 10000;
      GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
      ObjectMapper objectMapper = new ObjectMapper();
      Path dataFile = Paths.get("src/test/resources/multi-buffer.txt");

      Path gsf = Paths.get("target/multi-buffer.p1");

      double maxAllowedSpeedKnts = 0D;

      byte[] geoJsonBytes = null;
      byte[] wktBytes = null;

      GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
          geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
          row -> true, maxAllowedSpeedKnts);
      phase1.process();

      final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
      final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

      try (InputStream in = Files.newInputStream(gsf)) {
        GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
            objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
        );
        phase2.process(in, geoJsonOut, wktOut);
      }

      geoJsonBytes = geoJsonOut.toByteArray();
      wktBytes = wktOut.toByteArray();

      assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/multi-buffer.json")), objectMapper.readTree(geoJsonBytes), 0.00001);

      assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/multi-buffer.wkt"))), new String(wktBytes));
    }
  }

  @Test
  public void testMultiBuffer2() throws Exception {
    for (int simplifierBatchSize = 2; simplifierBatchSize <= 13; simplifierBatchSize++) {
      final int geoJsonPrecision = 5;
      final double simplificationTolerance = 0.01;
      final int NmSplit = 20;
      final int msSplit = 100000;
      final long maxCount = 10000;
      GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
      ObjectMapper objectMapper = new ObjectMapper();
      Path dataFile = Paths.get("src/test/resources/multi-buffer.txt");

      Path gsf = Paths.get("target/multi-buffer.p1");

      double maxAllowedSpeedKnts = 0D;

      byte[] geoJsonBytes = null;
      byte[] wktBytes = null;

      GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
          geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
          row -> true, maxAllowedSpeedKnts);
      phase1.process();

      final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
      final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

      try (InputStream in = Files.newInputStream(gsf)) {
        GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
            objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
        );
        phase2.process(in, geoJsonOut, wktOut);
      }

      geoJsonBytes = geoJsonOut.toByteArray();

      JsonNode geoJson = objectMapper.readTree(geoJsonBytes);
      ArrayNode coordinates = (ArrayNode) geoJson.at("/geometry/coordinates");

      ArrayNode firstSegment = (ArrayNode) coordinates.get(0);
      ArrayNode lastSegment = (ArrayNode) coordinates.get(coordinates.size() - 1);

      assertEquals(9.9601, firstSegment.get(0).get(0).doubleValue(), 0.00001);
      assertEquals(53.5301, firstSegment.get(0).get(1).doubleValue(), 0.00001);

      assertEquals(9.9612, lastSegment.get(lastSegment.size() - 1).get(0).doubleValue(), 0.00001);
      assertEquals(53.5302, lastSegment.get(lastSegment.size() - 1).get(1).doubleValue(), 0.00001);

    }
  }



  @Test
  public void testMultiBufferBadFirst() throws Exception {
    for (int simplifierBatchSize = 2; simplifierBatchSize <= 13; simplifierBatchSize++) {
      final int geoJsonPrecision = 5;
      final double simplificationTolerance = 0.000001;
      final int NmSplit = 100000;
      final int msSplit = 100000;
      final long maxCount = 10000;
      GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
      ObjectMapper objectMapper = new ObjectMapper();
      Path dataFile = Paths.get("src/test/resources/multi-buffer-bad-pt-first.txt");

      Path gsf = Paths.get("target/multi-buffer.p1");

      double maxAllowedSpeedKnts = 0D;

      byte[] geoJsonBytes = null;
      byte[] wktBytes = null;

      GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
          geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
          row -> true, maxAllowedSpeedKnts);
      phase1.process();

      final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
      final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

      try (InputStream in = Files.newInputStream(gsf)) {
        GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
            objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
        );
        phase2.process(in, geoJsonOut, wktOut);
      }

      geoJsonBytes = geoJsonOut.toByteArray();

      assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/multi-buffer-bad-pt-first.json")), objectMapper.readTree(geoJsonBytes),
          0.00001);
    }
  }

  @Test
  public void testMultiBufferBadLast() throws Exception {
    for (int simplifierBatchSize = 2; simplifierBatchSize <= 13; simplifierBatchSize++) {
      final int geoJsonPrecision = 5;
      final double simplificationTolerance = 0.000001;
      final int NmSplit = 20;
      final int msSplit = 100000;
      final long maxCount = 10000;
      GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
      ObjectMapper objectMapper = new ObjectMapper();
      Path dataFile = Paths.get("src/test/resources/multi-buffer-bad-pt-last.txt");

      Path gsf = Paths.get("target/multi-buffer.p1");

      double maxAllowedSpeedKnts = 0D;

      byte[] geoJsonBytes = null;
      byte[] wktBytes = null;

      GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
          geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
          row -> true, maxAllowedSpeedKnts);
      phase1.process();

      final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
      final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

      try (InputStream in = Files.newInputStream(gsf)) {
        GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
            objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
        );
        phase2.process(in, geoJsonOut, wktOut);
      }

      geoJsonBytes = geoJsonOut.toByteArray();

      assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/multi-buffer-bad-pt-first.json")), objectMapper.readTree(geoJsonBytes),
          0.00001);
    }
  }

  @Test
  public void testMultiBufferBadMid() throws Exception {
    for (int simplifierBatchSize = 2; simplifierBatchSize <= 13; simplifierBatchSize++) {
      final int geoJsonPrecision = 5;
      final double simplificationTolerance = 0.000001;
      final int NmSplit = 100000;
      final int msSplit = 100000;
      final long maxCount = 10000;
      GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
      ObjectMapper objectMapper = new ObjectMapper();
      Path dataFile = Paths.get("src/test/resources/multi-buffer-bad-pt-mid.txt");

      Path gsf = Paths.get("target/multi-buffer.p1");

      double maxAllowedSpeedKnts = 0D;

      byte[] geoJsonBytes = null;
      byte[] wktBytes = null;

      GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
          geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
          row -> true, maxAllowedSpeedKnts);
      phase1.process();

      final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
      final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

      try (InputStream in = Files.newInputStream(gsf)) {
        GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
            objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
        );
        phase2.process(in, geoJsonOut, wktOut);
      }

      geoJsonBytes = geoJsonOut.toByteArray();

      assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/multi-buffer-bad-pt-first.json")), objectMapper.readTree(geoJsonBytes),
          0.00001);
    }
  }


  @Test
  public void testLargeFileMultiPrecision() throws Exception {

    final int NmSplit = 0;
    final int msSplit = 0;
    final long maxCount = 10000;
    final long simplifierBatchSize = maxCount;

    final double simplificationTolerance = 0.01;

    final int geoJsonPrecision = 5;

    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
    ObjectMapper objectMapper = new ObjectMapper();
    Path dataFile = Paths.get("src/test/resources/data.txt");

    Path gsf = Paths.get("target/data.p1");

    double maxAllowedSpeedKnts = 0D;

    byte[] geoJsonBytes = null;
    byte[] wktBytes = null;

    GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
        geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, (int) simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
        row -> true, maxAllowedSpeedKnts);
    phase1.process();

    final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

    try (InputStream in = Files.newInputStream(gsf)) {
      GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
          objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
      );
      phase2.process(in, geoJsonOut, wktOut);
    }

    geoJsonBytes = geoJsonOut.toByteArray();

    System.out.println(new String(geoJsonBytes));
    assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/data.json")), objectMapper.readTree(geoJsonBytes), 0.00001);

  }

  @Test
  public void testSinglePoint2() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.0001;
    final int simplifierBatchSize = 3000;
    final int NmSplit = 20;
    final int msSplit = 3600000;
    final long maxCount = 10000;
    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
    ObjectMapper objectMapper = new ObjectMapper();
    Path dataFile = Paths.get("src/test/resources/single-point2.txt");

    Path gsf = Paths.get("target/single-point2.p1");

    double maxAllowedSpeedKnts = 0D;

    byte[] geoJsonBytes = null;
    byte[] wktBytes = null;

    GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
        geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
        row -> true, maxAllowedSpeedKnts);
    phase1.process();

    final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

    try (InputStream in = Files.newInputStream(gsf)) {
      GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
          objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
      );
      phase2.process(in, geoJsonOut, wktOut);
    }

    geoJsonBytes = geoJsonOut.toByteArray();
    wktBytes = wktOut.toByteArray();

    assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/single-point2.json")), objectMapper.readTree(geoJsonBytes), 0.00001);

    assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/single-point2.wkt"))), new String(wktBytes));

  }


  @Test
  public void testSinglePoint3() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.0001;
    final int simplifierBatchSize = 3000;
    final int NmSplit = 20;
    final int msSplit = 3600000;
    final long maxCount = 10000;
    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
    ObjectMapper objectMapper = new ObjectMapper();
    Path dataFile = Paths.get("src/test/resources/single-point3.txt");

    Path gsf = Paths.get("target/single-point3.p1");

    double maxAllowedSpeedKnts = 0D;

    byte[] geoJsonBytes = null;
    byte[] wktBytes = null;

    GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
        geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
        row -> true, maxAllowedSpeedKnts);
    phase1.process();

    final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

    try (InputStream in = Files.newInputStream(gsf)) {
      GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
          objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
      );
      phase2.process(in, geoJsonOut, wktOut);
    }

    geoJsonBytes = geoJsonOut.toByteArray();
    wktBytes = wktOut.toByteArray();

    assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/single-point3.json")), objectMapper.readTree(geoJsonBytes), 0.00001);

    assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/single-point3.wkt"))), new String(wktBytes));

  }

  @Test
  public void testSinglePoint4() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.0001;
    final int simplifierBatchSize = 3000;
    final int NmSplit = 20;
    final int msSplit = 3600000;
    final long maxCount = 10000;
    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
    ObjectMapper objectMapper = new ObjectMapper();
    Path dataFile = Paths.get("src/test/resources/single-point4.txt");

    Path gsf = Paths.get("target/single-point4.p1");

    double maxAllowedSpeedKnts = 0D;

    byte[] geoJsonBytes = null;
    byte[] wktBytes = null;

    GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
        geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
        row -> true, maxAllowedSpeedKnts);
    phase1.process();

    final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

    try (InputStream in = Files.newInputStream(gsf)) {
      GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
          objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
      );
      phase2.process(in, geoJsonOut, wktOut);
    }

    geoJsonBytes = geoJsonOut.toByteArray();
    wktBytes = wktOut.toByteArray();

    assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/single-point4.json")), objectMapper.readTree(geoJsonBytes), 0.00001);

    assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/single-point4.wkt"))), new String(wktBytes));

  }

  @Test
  public void testFilteredPoints() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.0001;
    final int simplifierBatchSize = 3000;
    final int NmSplit = 20;
    final int msSplit = 3600000;
    final long maxCount = 10000;
    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(simplificationTolerance);
    ObjectMapper objectMapper = new ObjectMapper();
    Path dataFile = Paths.get("src/test/resources/single-point4.txt");

    Path gsf = Paths.get("target/single-point4.p1");

    double maxAllowedSpeedKnts = 0D;

    byte[] geoJsonBytes = null;
    byte[] wktBytes = null;

    GeoSimplifierProcessor phase1 = new GeoSimplifierProcessor(
        geoJsonPrecision, NmSplit, msSplit, geometrySimplifier, simplifierBatchSize, dataFile, objectMapper, gsf, maxCount, geometryFactory,
        row -> false, maxAllowedSpeedKnts);
    phase1.process();

    final ByteArrayOutputStream geoJsonOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream wktOut = new ByteArrayOutputStream();

    try (InputStream in = Files.newInputStream(gsf)) {
      GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(
          objectMapper, geoJsonPrecision, maxAllowedSpeedKnts
      );
      phase2.process(in, geoJsonOut, wktOut);
    }

    geoJsonBytes = geoJsonOut.toByteArray();
    wktBytes = wktOut.toByteArray();

    assertJsonEquivalent(objectMapper.readTree(new File("src/test/resources/filtered-point4.json")), objectMapper.readTree(geoJsonBytes), 0.00001);

    assertEquals(new String(Files.readAllBytes(Paths.get("src/test/resources/filtered-point4.wkt"))), new String(wktBytes));

  }

}

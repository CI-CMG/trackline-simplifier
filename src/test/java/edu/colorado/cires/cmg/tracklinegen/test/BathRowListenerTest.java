package edu.colorado.cires.cmg.tracklinegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineWriter;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.GeoDataRow;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.GsBaseRowListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public class BathRowListenerTest {

  private final int maxCount = 0;
  private final int geoJsonPrecision = 4;
  private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
  final double tol = 0.0001;
  private final GeometrySimplifier geometrySimplifier = new GeometrySimplifier(tol);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private double maxAllowedSpeedKnts = 0D;

  @Test
  public void testEmpty() throws Exception {
    List<GeoDataRow> rows = Collections.emptyList();

    final long NmSplit = 2000L;
    final long msSplit = 1000L * 2L;
    final int batchSize = 5000;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/empty.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);

  }

  @Test
  public void testGeometryWithBigGap() throws Exception {
    Instant start = Instant.EPOCH;
    List<GeoDataRow> rows = Arrays.asList(
        new GeoDataRow(start, 0.0, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(0.0).withTimestamp(start).build(),
        new GeoDataRow(start.plusSeconds(1), 0.5, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0.5).withBathyTime(1.0).withTimestamp(start.plusSeconds(1)).build(),
        new GeoDataRow(start.plusSeconds(2), 0.6, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(0.6).withTimestamp(start.plusSeconds(2)).build(),
        new GeoDataRow(start.plusSeconds(3), 0.8, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0.8).withBathyTime(1.0).withTimestamp(start.plusSeconds(3)).build(),
        new GeoDataRow(start.plusSeconds(4), 0.9, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(0.9).withTimestamp(start.plusSeconds(4)).build(),

        new GeoDataRow(start.plusSeconds(8), 1.0, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1.0).withTimestamp(start.plusSeconds(8)).build(),
        new GeoDataRow(start.plusSeconds(9), 1.1, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(1.1).withBathyTime(1.0).withTimestamp(start.plusSeconds(9)).build(),
        new GeoDataRow(start.plusSeconds(10), 1.2, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1.2).withTimestamp(start.plusSeconds(10)).build(),
        new GeoDataRow(start.plusSeconds(11), 1.3, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(1.3).withBathyTime(1.0).withTimestamp(start.plusSeconds(11)).build(),
        new GeoDataRow(start.plusSeconds(12), 1.4, 0D)
//        DataRow.Builder.configure().withLat(0D).withLon(1.4).withTimestamp(start.plusSeconds(12)).build()
    );

    final long NmSplit = 2000L;
    final long msSplit = 1000L * 2L;
    final int batchSize = 5000;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/big-gap.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);

  }

  @Test
  public void testGeometryWithSmallGap() throws Exception {
    Instant start = Instant.EPOCH;
    List<GeoDataRow> rows = Arrays.asList(
        new GeoDataRow(start, 0.0, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(0.0).withTimestamp(start).build(),
        new GeoDataRow(start.plusSeconds(1), 0.5, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0.5).withBathyTime(1.0).withTimestamp(start.plusSeconds(1)).build(),
        new GeoDataRow(start.plusSeconds(2), 0.6, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(0.6).withTimestamp(start.plusSeconds(2)).build(),
        new GeoDataRow(start.plusSeconds(3), 0.8, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0.8).withBathyTime(1.0).withTimestamp(start.plusSeconds(3)).build(),
        new GeoDataRow(start.plusSeconds(4), 0.9, 0D)
//        DataRow.Builder.configure().withLat(0D).withLon(0.9).withTimestamp(start.plusSeconds(4)).build()
    );

    final long NmSplit = 2000L;
    final long msSplit = 1000L * 2L;
    final int batchSize = 5000;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/small-gap.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);
  }


  @Test
  public void testBatchExceeded() throws Exception {
    Instant start = Instant.EPOCH;
    List<GeoDataRow> rows = Arrays.asList(
        new GeoDataRow(start, 0D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0D).withBathyTime(1.0).withTimestamp(start).build(),
        new GeoDataRow(start.plusSeconds(1), 1D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1D).withTimestamp(start.plusSeconds(1)).build(),
        new GeoDataRow(start.plusSeconds(2), 2D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(2D).withBathyTime(1.0).withTimestamp(start.plusSeconds(2)).build(),
        new GeoDataRow(start.plusSeconds(3), 3D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(3D).withTimestamp(start.plusSeconds(3)).build(),
        new GeoDataRow(start.plusSeconds(4), 4D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(4D).withBathyTime(1.0).withTimestamp(start.plusSeconds(4)).build(),
        new GeoDataRow(start.plusSeconds(5), 5D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(5D).withTimestamp(start.plusSeconds(5)).build(),
        new GeoDataRow(start.plusSeconds(6), 6D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(6D).withBathyTime(1.0).withTimestamp(start.plusSeconds(6)).build(),
        new GeoDataRow(start.plusSeconds(7), 7D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(7D).withTimestamp(start.plusSeconds(7)).build(),
        new GeoDataRow(start.plusSeconds(8), 8D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(8D).withBathyTime(1.0).withTimestamp(start.plusSeconds(8)).build(),
        new GeoDataRow(start.plusSeconds(9), 9D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(9D).withTimestamp(start.plusSeconds(9)).build(),
        new GeoDataRow(start.plusSeconds(10), 10D, 0D, 1.0)
//        DataRow.Builder.configure().withLat(0D).withLon(10D).withBathyTime(1.0).withTimestamp(start.plusSeconds(10)).build()
    );

    final long NmSplit = 2000L;
    final long msSplit = 1000L * 2L;
    final int batchSize = 6;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/batch.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);

  }

  @Test
  public void testBatchExceededSplit2() throws Exception {
    Instant start = Instant.EPOCH;
    List<GeoDataRow> rows = Arrays.asList(
        new GeoDataRow(start, 0D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0D).withBathyTime(1.0).withTimestamp(start).build(),
        new GeoDataRow(start.plusSeconds(1), 1D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1D).withTimestamp(start.plusSeconds(1)).build(),
        new GeoDataRow(start.plusSeconds(2), 2D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(2D).withBathyTime(1.0).withTimestamp(start.plusSeconds(2)).build(),
        new GeoDataRow(start.plusSeconds(3), 3D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(3D).withTimestamp(start.plusSeconds(3)).build(),
        new GeoDataRow(start.plusSeconds(4), 4D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(4D).withBathyTime(1.0).withTimestamp(start.plusSeconds(4)).build(),

        new GeoDataRow(start.plusSeconds(5), 5D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(5D).withTimestamp(start.plusSeconds(5)).build(),
        new GeoDataRow(start.plusSeconds(6), 6D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(6D).withBathyTime(1.0).withTimestamp(start.plusSeconds(6)).build(),
        new GeoDataRow(start.plusSeconds(7), 7D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(7D).withTimestamp(start.plusSeconds(7)).build(),
        new GeoDataRow(start.plusSeconds(8), 8D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(8D).withBathyTime(1.0).withTimestamp(start.plusSeconds(8)).build(),

        new GeoDataRow(start.plusSeconds(9), 9D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(9D).withTimestamp(start.plusSeconds(9)).build(),
        new GeoDataRow(start.plusSeconds(10), 10D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(10D).withTimestamp(start.plusSeconds(10)).build(),

        new GeoDataRow(start.plusSeconds(11), 11D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(11D).withBathyTime(1.0).withTimestamp(start.plusSeconds(11)).build(),
        new GeoDataRow(start.plusSeconds(12), 12D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(12D).withBathyTime(1.0).withTimestamp(start.plusSeconds(12)).build(),
        new GeoDataRow(start.plusSeconds(13), 13D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(13D).withTimestamp(start.plusSeconds(13)).build(),
        new GeoDataRow(start.plusSeconds(14), 14D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(14D).withBathyTime(1.0).withTimestamp(start.plusSeconds(14)).build(),
        new GeoDataRow(start.plusSeconds(15), 15D, 0D, 1.0)
//        DataRow.Builder.configure().withLat(0D).withLon(15D).withBathyTime(1.0).withTimestamp(start.plusSeconds(15)).build()

    );
    final long NmSplit = 2000L;
    final long msSplit = 1000L * 2L;
    final int batchSize = 3;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/batch-split2.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);

  }

  @Test
  public void testBatchExceededSplit() throws Exception {
    Instant start = Instant.EPOCH;
    List<GeoDataRow> rows = Arrays.asList(
        new GeoDataRow(start, 0D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0D).withBathyTime(1.0).withTimestamp(start).build(),
        new GeoDataRow(start.plusSeconds(1), 1D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1D).withTimestamp(start.plusSeconds(1)).build(),
        new GeoDataRow(start.plusSeconds(2), 2D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(2D).withBathyTime(1.0).withTimestamp(start.plusSeconds(2)).build(),
        new GeoDataRow(start.plusSeconds(3), 3D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(3D).withTimestamp(start.plusSeconds(3)).build(),
        new GeoDataRow(start.plusSeconds(4), 4D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(4D).withBathyTime(1.0).withTimestamp(start.plusSeconds(4)).build(),

        new GeoDataRow(start.plusSeconds(5), 5D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(5D).withTimestamp(start.plusSeconds(5)).build(),
        new GeoDataRow(start.plusSeconds(6), 6D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(6D).withBathyTime(1.0).withTimestamp(start.plusSeconds(6)).build(),
        new GeoDataRow(start.plusSeconds(7), 7D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(7D).withTimestamp(start.plusSeconds(7)).build(),
        new GeoDataRow(start.plusSeconds(8), 8D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(8D).withBathyTime(1.0).withTimestamp(start.plusSeconds(8)).build(),

        new GeoDataRow(start.plusSeconds(9), 9D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(9D).withTimestamp(start.plusSeconds(9)).build(),
        new GeoDataRow(start.plusSeconds(10), 10D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(10D).withTimestamp(start.plusSeconds(10)).build(),

        new GeoDataRow(start.plusSeconds(11), 11D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(11D).withBathyTime(1.0).withTimestamp(start.plusSeconds(11)).build(),
        new GeoDataRow(start.plusSeconds(12), 12D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(12D).withBathyTime(1.0).withTimestamp(start.plusSeconds(12)).build(),
        new GeoDataRow(start.plusSeconds(13), 13D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(13D).withTimestamp(start.plusSeconds(13)).build(),
        new GeoDataRow(start.plusSeconds(14), 14D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(14D).withBathyTime(1.0).withTimestamp(start.plusSeconds(14)).build(),
        new GeoDataRow(start.plusSeconds(15), 15D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(15D).withTimestamp(start.plusSeconds(15)).build(),
        new GeoDataRow(start.plusSeconds(16), 16D, 0D, 1.0)
//        DataRow.Builder.configure().withLat(0D).withLon(16D).withBathyTime(1.0).withTimestamp(start.plusSeconds(16)).build()

    );

    final long NmSplit = 2000L;
    final long msSplit = 1000L * 2L;
    final int batchSize = 3;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/batch-split.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);

  }

  @Test
  public void testBatchExceededSplit3() throws Exception {
    Instant start = Instant.EPOCH;
    List<GeoDataRow> rows = Arrays.asList(
        new GeoDataRow(start, 0D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0D).withBathyTime(1.0).withTimestamp(start).build(),
        new GeoDataRow(start.plusSeconds(1), 1D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1D).withTimestamp(start.plusSeconds(1)).build(),
        new GeoDataRow(start.plusSeconds(2), 2D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(2D).withBathyTime(1.0).withTimestamp(start.plusSeconds(2)).build(),
        new GeoDataRow(start.plusSeconds(3), 3D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(3D).withTimestamp(start.plusSeconds(3)).build(),
        new GeoDataRow(start.plusSeconds(4), 4D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(4D).withBathyTime(1.0).withTimestamp(start.plusSeconds(4)).build(),

        new GeoDataRow(start.plusSeconds(5), 5D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(5D).withTimestamp(start.plusSeconds(5)).build(),
        new GeoDataRow(start.plusSeconds(6), 6D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(6D).withBathyTime(1.0).withTimestamp(start.plusSeconds(6)).build(),
        new GeoDataRow(start.plusSeconds(7), 7D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(7D).withTimestamp(start.plusSeconds(7)).build(),
        new GeoDataRow(start.plusSeconds(8), 8D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(8D).withBathyTime(1.0).withTimestamp(start.plusSeconds(8)).build(),

        new GeoDataRow(start.plusSeconds(9), 9D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(9D).withTimestamp(start.plusSeconds(9)).build(),
        new GeoDataRow(start.plusSeconds(10), 10D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(10D).withTimestamp(start.plusSeconds(10)).build(),

        new GeoDataRow(start.plusSeconds(11), 11D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(11D).withBathyTime(1.0).withTimestamp(start.plusSeconds(11)).build(),
        new GeoDataRow(start.plusSeconds(12), 12D, 0D, 1.0)
//        DataRow.Builder.configure().withLat(0D).withLon(12D).withBathyTime(1.0).withTimestamp(start.plusSeconds(12)).build()

    );
    final long NmSplit = 2000L;
    final long msSplit = 1000L * 2L;
    final int batchSize = 3;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/batch-split3.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);

  }

  @Test
  public void testBatchExceededSplit4() throws Exception {
    Instant start = Instant.EPOCH;
    List<GeoDataRow> rows = Arrays.asList(
        new GeoDataRow(start, 0D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0D).withBathyTime(1.0).withTimestamp(start).build(),
        new GeoDataRow(start.plusSeconds(1), 1D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1D).withTimestamp(start.plusSeconds(1)).build(),
        new GeoDataRow(start.plusSeconds(2), 2D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(2D).withBathyTime(1.0).withTimestamp(start.plusSeconds(2)).build(),
        new GeoDataRow(start.plusSeconds(3), 3D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(3D).withTimestamp(start.plusSeconds(3)).build(),
        new GeoDataRow(start.plusSeconds(4), 4D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(4D).withBathyTime(1.0).withTimestamp(start.plusSeconds(4)).build(),

        new GeoDataRow(start.plusSeconds(5), 5D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(5D).withTimestamp(start.plusSeconds(5)).build(),
        new GeoDataRow(start.plusSeconds(6), 6D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(6D).withBathyTime(1.0).withTimestamp(start.plusSeconds(6)).build(),
        new GeoDataRow(start.plusSeconds(7), 7D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(7D).withTimestamp(start.plusSeconds(7)).build(),
        new GeoDataRow(start.plusSeconds(8), 8D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(8D).withBathyTime(1.0).withTimestamp(start.plusSeconds(8)).build(),

        new GeoDataRow(start.plusSeconds(9), 9D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(9D).withTimestamp(start.plusSeconds(9)).build(),
        new GeoDataRow(start.plusSeconds(10), 10D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(10D).withTimestamp(start.plusSeconds(10)).build(),

        new GeoDataRow(start.plusSeconds(11), 11D, 0D, 1.0)
//        DataRow.Builder.configure().withLat(0D).withLon(11D).withBathyTime(1.0).withTimestamp(start.plusSeconds(11)).build()

    );
    final long NmSplit = 2000L;
    final long msSplit = 1000L * 2L;
    final int batchSize = 3;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/batch-split4.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);

  }

  @Test
  public void testAllSinglePoints() throws Exception {
    Instant start = Instant.EPOCH;
    List<GeoDataRow> rows = Arrays.asList(
        new GeoDataRow(start, 0D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0D).withBathyTime(1.0).withTimestamp(start).build(),
        new GeoDataRow(start.plusSeconds(1), 1D, 0D, 1.0),
//        DataRow.Builder.configure().withLat(1D).withLon(0D).withBathyTime(1.0).withTimestamp(start.plusSeconds(1)).build(),
        new GeoDataRow(start.plusSeconds(2), 2D, 0D, 1.0)
//        DataRow.Builder.configure().withLat(1D).withLon(2D).withBathyTime(1.0).withTimestamp(start.plusSeconds(2)).build()
    );

    final long NmSplit = 2000L;
    final long msSplit = 10;
    final int batchSize = 100;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/empty-sparse.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);

  }

  @Test
  public void testGeometryBadNav() throws Exception {
    Instant start = Instant.EPOCH;
    List<GeoDataRow> rows = Arrays.asList(
        new GeoDataRow(start, 0D, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(0.0).withTimestamp(start).build(),
        new GeoDataRow(start.plusSeconds(1), 0.5, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0.5).withBathyTime(1.0).withTimestamp(start.plusSeconds(1)).build(),
        new GeoDataRow(start.plusSeconds(2), 0.6, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(0.6).withTimestamp(start.plusSeconds(2)).build(),
        new GeoDataRow(start.plusSeconds(3), 0.8, 0D, 1.0),
//        DataRow.Builder.configure().withLat(0D).withLon(0.8).withBathyTime(1.0).withTimestamp(start.plusSeconds(3)).build(),
        new GeoDataRow(start.plusSeconds(4), 0.9, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(0.9).withTimestamp(start.plusSeconds(4)).build(),

        new GeoDataRow(start.plusSeconds(8), 1.0, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1.0).withBathyQualityCode(6).withTimestamp(start.plusSeconds(8)).build(),
        new GeoDataRow(start.plusSeconds(9), 1.1, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1.1).withBathyQualityCode(6).withBathyTime(1.0).withTimestamp(start.plusSeconds(9)).build(),
        new GeoDataRow(start.plusSeconds(10), 1.2, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1.2).withBathyQualityCode(6).withTimestamp(start.plusSeconds(10)).build(),
        new GeoDataRow(start.plusSeconds(11), 1.3, 0D),
//        DataRow.Builder.configure().withLat(0D).withLon(1.3).withBathyQualityCode(6).withBathyTime(1.0).withTimestamp(start.plusSeconds(11)).build(),
        new GeoDataRow(start.plusSeconds(12), 1.4, 0D)
//        DataRow.Builder.configure().withLat(0D).withLon(1.4).withBathyQualityCode(6).withTimestamp(start.plusSeconds(12)).build()
    );

    final long NmSplit = 2000L;
    final long msSplit = 1000L * 2L;
    final int batchSize = 5000;

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
      GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
      GsBaseRowListener listener = new GsBaseRowListener(
          NmSplit,
          msSplit,
          geometrySimplifier,
          lineWriter,
          batchSize,
          maxCount,
          geometryFactory,
          geoJsonPrecision,
          r -> r.getBathyTime() != null,
          maxAllowedSpeedKnts
      );
      listener.start();
      rows.forEach(listener::processRow);
      listener.finish();
    }

    JsonNode expected;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream("bath/basic.json")) {
      expected = objectMapper.readTree(in);
    }

    JsonNode geoJson = objectMapper.readTree(out.toByteArray());

    assertEquals(expected, geoJson);

  }

}
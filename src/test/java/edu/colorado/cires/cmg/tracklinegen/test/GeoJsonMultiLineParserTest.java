package edu.colorado.cires.cmg.tracklinegen.test;

import static edu.colorado.cires.cmg.tracklinegen.JsonPropertiesUtils.assertJsonEquivalent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineParser;
import edu.colorado.cires.cmg.tracklinegen.ValidationException;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.jackson.ObjectMapperCreator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

public class GeoJsonMultiLineParserTest {

  private final ObjectMapper objectMapper = ObjectMapperCreator.create();

  @Test
  public void testSplit() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream wktOut = new ByteArrayOutputStream();
    JsonNode expected;
    try (JsonParser jsonParser = objectMapper.getFactory()
      .createParser(getClass().getClassLoader().getResourceAsStream("am/am1.json"));
      JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out);
      PrintWriter wktWriter = new PrintWriter(wktOut);
      InputStream expIn = getClass().getClassLoader().getResourceAsStream("am/am1-expected.json")
    ) {
      expected = objectMapper.readTree(expIn);
      GeoJsonMultiLineParser parser = new GeoJsonMultiLineParser(objectMapper, 4, 0D);
      parser.parse(jsonParser, jsonGenerator, wktWriter);
    }

    JsonNode split = objectMapper.readTree(out.toByteArray());
    assertEquals(expected, split);
  }

  @Test
  public void testNoSplit() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream wktOut = new ByteArrayOutputStream();
    JsonNode expected;
    try (JsonParser jsonParser = objectMapper.getFactory()
      .createParser(getClass().getClassLoader().getResourceAsStream("am/am2.json"));
      JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out);
      PrintWriter wktWriter = new PrintWriter(wktOut);
      InputStream expIn = getClass().getClassLoader().getResourceAsStream("am/am2-expected.json")
    ) {
      expected = objectMapper.readTree(expIn);
      GeoJsonMultiLineParser parser = new GeoJsonMultiLineParser(objectMapper, 4, 0D);
      parser.parse(jsonParser, jsonGenerator, wktWriter);
    }
    JsonNode split = objectMapper.readTree(out.toByteArray());
    assertEquals(expected, split);

  }

  @Test
  public void testEmpty() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream wktOut = new ByteArrayOutputStream();
    JsonNode expected;
    try (JsonParser jsonParser = objectMapper.getFactory()
      .createParser(getClass().getClassLoader().getResourceAsStream("am/empty.json"));
      JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out);
      PrintWriter wktWriter = new PrintWriter(wktOut);
      InputStream expIn = getClass().getClassLoader().getResourceAsStream("am/empty-expected.json")
    ) {
      expected = objectMapper.readTree(expIn);
      GeoJsonMultiLineParser parser = new GeoJsonMultiLineParser(objectMapper, 4, 0D);
      parser.parse(jsonParser, jsonGenerator, wktWriter);
    }

    JsonNode split = objectMapper.readTree(out.toByteArray());
    assertEquals(expected, split);

  }

//  @Test
//  public void testStopOnAntimeridian() throws Exception {
//    ByteArrayOutputStream out = new ByteArrayOutputStream();
//    ByteArrayOutputStream wktOut = new ByteArrayOutputStream();
//    JsonNode expected;
//    try (JsonParser jsonParser = objectMapper.getFactory().createParser(getClass().getClassLoader().getResourceAsStream("am/am3.json"));
//        JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out);
//        PrintWriter wktWriter = new PrintWriter(wktOut);
//        InputStream expIn = getClass().getClassLoader().getResourceAsStream("am/am3-expected.json")
//    ) {
//      expected = objectMapper.readTree(expIn);
//      GeoJsonMultiLineParser parser = new GeoJsonMultiLineParser(objectMapper, 4, 0D);
//      parser.parse(jsonParser, jsonGenerator, wktWriter);
//    }
//
//    JsonNode split = objectMapper.readTree(out.toByteArray());
//    assertEquals(expected, split);
//  }

  @Test
  void testAllowDuplicateTimestampsSettingEnabled() throws IOException, ValidationException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream wktOut = new ByteArrayOutputStream();
    JsonNode expectedGeoJson;
    try (JsonParser jsonParser = objectMapper.getFactory()
      .createParser(getClass().getClassLoader().getResourceAsStream(
        "duplicate-timestamps/input.json"));
      JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out);
      PrintWriter wktWriter = new PrintWriter(wktOut);
      InputStream expIn = getClass().getClassLoader()
        .getResourceAsStream("duplicate-timestamps/expected.json")
    ) {
      expectedGeoJson = objectMapper.readTree(expIn);
      GeoJsonMultiLineParser parser = new GeoJsonMultiLineParser(objectMapper, 4, 60D, true);
      parser.parse(jsonParser, jsonGenerator, wktWriter);
    }

    JsonNode resultGeoJson = objectMapper.readTree(out.toByteArray());
    assertJsonEquivalent(expectedGeoJson, resultGeoJson, 4);

    String resultWKT = wktOut.toString();
    String expectedWKT = FileUtils.readFileToString(
      Paths.get("src/test/resources/duplicate-timestamps/expected.wkt").toFile(),
      StandardCharsets.UTF_8);
    assertEquals(expectedWKT, resultWKT);
  }

  @Test
  void testAllowDuplicateTimestampsSettingDisabled() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream wktOut = new ByteArrayOutputStream();
    try (JsonParser jsonParser = objectMapper.getFactory()
      .createParser(getClass().getClassLoader().getResourceAsStream(
        "duplicate-timestamps/input.json"));
      JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out);
      PrintWriter wktWriter = new PrintWriter(wktOut)
    ) {
      GeoJsonMultiLineParser parser = new GeoJsonMultiLineParser(objectMapper, 4,
        60D); // allowDuplicateTimestamps set to false by default
      assertThrows(ValidationException.class,
        () -> parser.parse(jsonParser, jsonGenerator, wktWriter));
    }
  }
}
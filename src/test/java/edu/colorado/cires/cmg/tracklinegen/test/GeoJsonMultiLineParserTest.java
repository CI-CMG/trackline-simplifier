package edu.colorado.cires.cmg.tracklinegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineParser;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.jackson.ObjectMapperCreator;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class GeoJsonMultiLineParserTest {

  private final ObjectMapper objectMapper = ObjectMapperCreator.create();

  @Test
  public void testSplit() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream wktOut = new ByteArrayOutputStream();
    JsonNode expected;
    try (JsonParser jsonParser = objectMapper.getFactory().createParser(getClass().getClassLoader().getResourceAsStream("am/am1.json"));
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
    try (JsonParser jsonParser = objectMapper.getFactory().createParser(getClass().getClassLoader().getResourceAsStream("am/am2.json"));
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
    try (JsonParser jsonParser = objectMapper.getFactory().createParser(getClass().getClassLoader().getResourceAsStream("am/empty.json"));
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

}
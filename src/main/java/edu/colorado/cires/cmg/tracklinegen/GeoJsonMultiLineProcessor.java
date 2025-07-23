package edu.colorado.cires.cmg.tracklinegen;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GeoJsonMultiLineProcessor {

  private final ObjectMapper objectMapper;
  private final int geoJsonPrecision;
  private final double maxAllowedSpeedKnts;
  private final boolean allowDuplicateTimestamps;

  public GeoJsonMultiLineProcessor(ObjectMapper objectMapper, int geoJsonPrecision, double maxAllowedSpeedKnts,  boolean allowDuplicateTimestamps) {
    this.objectMapper = objectMapper;
    this.geoJsonPrecision = geoJsonPrecision;
    this.maxAllowedSpeedKnts = maxAllowedSpeedKnts;
    this.allowDuplicateTimestamps = allowDuplicateTimestamps;
  }

  public GeoJsonMultiLineProcessor(ObjectMapper objectMapper, int geoJsonPrecision, double maxAllowedSpeedKnts) {
    this(objectMapper, geoJsonPrecision, maxAllowedSpeedKnts, false);
  }

  public void process(InputStream in, OutputStream out, OutputStream wktOut, Map<String, Object> additionalProperties) throws ValidationException {
    GeoJsonMultiLineParser parser = new GeoJsonMultiLineParser(objectMapper, geoJsonPrecision, maxAllowedSpeedKnts, allowDuplicateTimestamps);
    try (
        JsonParser jsonParser = getJsonParser(in);
        JsonGenerator jsonGenerator = getGenerator(out);
        Writer wktWriter = new OutputStreamWriter(wktOut, StandardCharsets.UTF_8)) {

      parser.parse(jsonParser, jsonGenerator, wktWriter, additionalProperties);

    } catch (IOException e) {
      throw new RuntimeException("Unable to process geometry", e);
    }
  }

  public void process(InputStream in, OutputStream out, OutputStream wktOut) throws ValidationException {
    process(in, out, wktOut, null);
  }

  private JsonGenerator getGenerator(OutputStream out) {
    try {
      return objectMapper.getFactory().createGenerator(out);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create JSON generator", e);
    }
  }

  private JsonParser getJsonParser(InputStream in){
    try {
      return objectMapper.getFactory().createParser(in);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create JSON parser", e);
    }
  }
}


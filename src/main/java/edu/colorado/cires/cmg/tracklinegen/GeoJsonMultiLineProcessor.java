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

public class GeoJsonMultiLineProcessor {

  private final ObjectMapper objectMapper;
  private final int geoJsonPrecision;
  private final double maxAllowedSpeedKnts;

  public GeoJsonMultiLineProcessor(ObjectMapper objectMapper, int geoJsonPrecision, double maxAllowedSpeedKnts) {
    this.objectMapper = objectMapper;
    this.geoJsonPrecision = geoJsonPrecision;
    this.maxAllowedSpeedKnts = maxAllowedSpeedKnts;
  }


  public void process(InputStream in, OutputStream out, OutputStream wktOut) throws ValidationException {
    GeoJsonMultiLineParser parser = new GeoJsonMultiLineParser(objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);
    try (
        JsonParser jsonParser = getJsonParser(in);
        JsonGenerator jsonGenerator = getGenerator(out);
        Writer wktWriter = new OutputStreamWriter(wktOut, StandardCharsets.UTF_8)) {

      parser.parse(jsonParser, jsonGenerator, wktWriter);

    } catch (IOException e) {
      throw new RuntimeException("Unable to process geometry", e);
    }
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


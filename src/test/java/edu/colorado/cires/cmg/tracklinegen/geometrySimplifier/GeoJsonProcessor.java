package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineParser;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import edu.colorado.cires.cmg.tracklinegen.ValidationException;
import edu.colorado.cires.cmg.tracklinegen.ValidationRuntimeException;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeoJsonProcessor implements Closeable {
  private int geoJsonPrecision;
  private double maxAllowedSpeedKnts;

  private ObjectMapper objectMapper;
  private JsonParser jsonParser;
  private JsonGenerator jsonGenerator;
  private OutputStreamWriter wktWriter;

  public GeoJsonProcessor(Path gsf, Path geoJsonFile, Path wktFile, ObjectMapper objectMapper, int geoJsonPrecision, double maxAllowedSpeedKnts) {
    this.geoJsonPrecision = geoJsonPrecision;
    this.maxAllowedSpeedKnts = maxAllowedSpeedKnts;
    this.objectMapper = objectMapper;
    try {
      jsonParser = objectMapper.getFactory().createParser(Files.newInputStream(gsf));
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      jsonGenerator = objectMapper.getFactory().createGenerator(Files.newOutputStream(geoJsonFile));
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      wktWriter = new OutputStreamWriter(Files.newOutputStream(wktFile), StandardCharsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public void process(){
    GeoJsonMultiLineParser parser = new GeoJsonMultiLineParser(objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);
    try {
      parser.parse(jsonParser, jsonGenerator, wktWriter);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ValidationException e) {
      throw new ValidationRuntimeException(e);
    }
    close();
  }

  @Override
  public void close() {
    try {
      jsonParser.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      jsonGenerator.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    try {
      wktWriter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

  }
}

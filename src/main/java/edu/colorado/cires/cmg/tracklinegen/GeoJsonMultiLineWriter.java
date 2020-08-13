package edu.colorado.cires.cmg.tracklinegen;

import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GeoJsonMultiLineWriter {

  private final JsonGenerator jsonGenerator;
  private final DecimalFormat format;

  public GeoJsonMultiLineWriter(JsonGenerator jsonGenerator, int precision) {
    this.jsonGenerator = jsonGenerator;
    StringBuilder sb = new StringBuilder("0.");
    for (int i = 1; i <= precision; i++) {
      sb.append("#");
    }
    format = new DecimalFormat(sb.toString(), DecimalFormatSymbols.getInstance(Locale.ENGLISH));
  }

  public void start() {
    try {
      jsonGenerator.writeStartObject();
      jsonGenerator.writeFieldName("type");
      jsonGenerator.writeString("Feature");
      jsonGenerator.writeFieldName("geometry");
      jsonGenerator.writeStartObject();
      jsonGenerator.writeFieldName("type");
      jsonGenerator.writeString("MultiLineString");
      jsonGenerator.writeFieldName("coordinates");
      jsonGenerator.writeStartArray();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write GeoJson", e);
    }
  }

  public void finish(GeometryProperties properties) {
    try {
      jsonGenerator.writeEndArray();
      jsonGenerator.writeEndObject();
      jsonGenerator.writeFieldName("properties");
      jsonGenerator.writeObject(properties);
      jsonGenerator.writeEndObject();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write GeoJson", e);
    }
  }

  public void endLine() {
    try {
      jsonGenerator.writeEndArray();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write GeoJson", e);
    }
  }

  public void startLine() {
    try {
      jsonGenerator.writeStartArray();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write GeoJson", e);
    }
  }

  public void writeCoordinate(List<Double> values) {
    try {
      jsonGenerator.writeStartArray();
      for (Double value : values) {
        jsonGenerator.writeNumber(format.format(value));
      }
      jsonGenerator.writeEndArray();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write GeoJson", e);
    }
  }

  public void writeCoordinate(double lon, double lat, double... additional) {
    List<Double> values = Arrays.asList(lon, lat);
    for (double d : additional) {
      values.add(d);
    }
    writeCoordinate(values);
  }


}

package edu.colorado.cires.cmg.tracklinegen;

import static edu.colorado.cires.cmg.tracklinegen.AntimeridianUtils.getDistance;
import static edu.colorado.cires.cmg.tracklinegen.AntimeridianUtils.getSpeed;
import static edu.colorado.cires.cmg.tracklinegen.AntimeridianUtils.splitAm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

public class GeoJsonMultiLineParser {

  private static final TypeReference<List<Double>> LIST_DOUBLE = new TypeReference<List<Double>>() {
  };

  private final ObjectMapper objectMapper;
  private final GeometryFactory geometryFactory = GeometryFactoryFactory.create();
  private final DecimalFormat format;
  private final double maxAllowedSpeedKnts;

  private Envelope westBoundingBox = new Envelope();
  private Envelope eastBoundingBox = new Envelope();
  private boolean crossedAntimeridian = false;
  private double distanceM = 0;
  private long count = 0;
  private Double avgSpeedM = 0.0;
  private GeometryProperties properties = null;
  private ArrayList<Double> bbox = null;
  private Coordinate lastCoordinate = null;

  public GeoJsonMultiLineParser(ObjectMapper objectMapper, int precision, double maxAllowedSpeedKnts) {
    this.objectMapper = objectMapper;
    StringBuilder sb = new StringBuilder("0.");
    for (int i = 1; i <= precision; i++) {
      sb.append("#");
    }
    format = new DecimalFormat(sb.toString(), DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    this.maxAllowedSpeedKnts = maxAllowedSpeedKnts;
  }


  private void copyInternal(JsonParser jsonParser, JsonGenerator jsonGenerator) throws IOException {
    switch (jsonParser.getCurrentToken()) {
      case START_OBJECT:
        copyObject(jsonParser, jsonGenerator);
        break;
      case START_ARRAY:
        copyArray(jsonParser, jsonGenerator);
        break;
      default:
        jsonGenerator.copyCurrentEvent(jsonParser);
        break;
    }
  }

  private void copyArray(JsonParser jsonParser, JsonGenerator jsonGenerator) throws IOException {
    jsonGenerator.copyCurrentEvent(jsonParser); //start array
    while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
      copyInternal(jsonParser, jsonGenerator);
    }
    jsonGenerator.copyCurrentEvent(jsonParser); //end array
  }

  private void copyObject(JsonParser jsonParser, JsonGenerator jsonGenerator) throws IOException {
    jsonGenerator.copyCurrentEvent(jsonParser); //start object
    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
      copyInternal(jsonParser, jsonGenerator);
    }
    jsonGenerator.copyCurrentEvent(jsonParser); //end object
  }

  public void parse(JsonParser jsonParser, JsonGenerator jsonGenerator, Writer wktWriter) throws IOException, ValidationException {
    parse(jsonParser, jsonGenerator, wktWriter, null);
  }

  public void parse(JsonParser jsonParser, JsonGenerator jsonGenerator, Writer wktWriter, Map<String, Object> additionalProperties)
      throws IOException, ValidationException {

    jsonParser.nextToken();
    jsonGenerator.copyCurrentEvent(jsonParser); //start object
    if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT) {
      throw new IllegalArgumentException("Not a JSON object");
    }

    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {

      String fieldName = jsonParser.getCurrentName();
      if ("type".equals(fieldName)) {
        jsonGenerator.copyCurrentEvent(jsonParser);
        verifyFeatureType(jsonParser, jsonGenerator, "Feature");
      } else if ("geometry".equals(fieldName)) {
        jsonGenerator.copyCurrentEvent(jsonParser);
        processGeometry(jsonParser, jsonGenerator, wktWriter);
      } else if ("properties".equals(fieldName)) {
        jsonParser.nextToken();
        properties = objectMapper.readValue(jsonParser, GeometryProperties.class);
      } else {
        jsonGenerator.copyCurrentEvent(jsonParser);
        copyEverythingElse(jsonParser, jsonGenerator);
      }
    }

    if (bbox != null) {
      jsonGenerator.writeFieldName("bbox");
      objectMapper.writeValue(jsonGenerator, bbox);
    }

    if (properties == null) {
      properties = GeometryProperties.Builder.configure().build();
    }

    GeometryProperties.Builder propertiesBuilder;

    if (avgSpeedM.isNaN()) {
      propertiesBuilder = GeometryProperties.Builder.configure(properties)
              .withDistanceM(distanceM);
    }
    else {
      propertiesBuilder = GeometryProperties.Builder.configure(properties)
              .withDistanceM(distanceM)
              .withAvgSpeedMPS(avgSpeedM);
    }


    if (additionalProperties != null) {
      for (Entry<String, Object> entry : additionalProperties.entrySet()) {
        propertiesBuilder.withOtherField(entry.getKey(), entry.getValue());
      }
    }

    properties = propertiesBuilder.build();

    jsonGenerator.writeFieldName("properties");
    objectMapper.writeValue(jsonGenerator, properties);

    jsonGenerator.writeEndObject();

  }

  private void processGeometry(JsonParser jsonParser, JsonGenerator jsonGenerator, Writer wktWriter) throws IOException, ValidationException {
    jsonParser.nextToken();
    jsonGenerator.copyCurrentEvent(jsonParser); //start object
    if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT) {
      throw new IllegalArgumentException("Geometry was not an object");
    }

    String featureType = null;
    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
      jsonGenerator.copyCurrentEvent(jsonParser);
      String fieldName = jsonParser.getCurrentName();
      if ("type".equals(fieldName)) {
        featureType = verifyFeatureType(jsonParser, jsonGenerator, "MultiLineString", "Point");
        if (featureType.equals("MultiLineString")) {
          wktWriter.write("MULTILINESTRING ");
        } else {
          wktWriter.write("POINT ");
        }
      } else if ("coordinates".equals(fieldName)) {
        processCoordinates(jsonParser, jsonGenerator, wktWriter, featureType);
        writeBBox(jsonGenerator);
      } else {
        copyEverythingElse(jsonParser, jsonGenerator);
      }
    }
    jsonGenerator.copyCurrentEvent(jsonParser); //end object
  }

  private void processCoordinates(JsonParser jsonParser, JsonGenerator jsonGenerator, Writer wktWriter, String type)
      throws IOException, ValidationException {
    if (type.equals("MultiLineString")) {
      long lineStringCounter = 0;
      jsonParser.nextToken();
      jsonGenerator.copyCurrentEvent(jsonParser); //start array
      wktWriter.write("("); //start multi line string
      if (jsonParser.getCurrentToken() != JsonToken.START_ARRAY) {
        throw new IllegalArgumentException("coordinates was not an array");
      }
      while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
        if (lineStringCounter > 0) {
          wktWriter.write(","); //line string separator
        }
        processLineString(jsonParser, jsonGenerator, wktWriter);
        lineStringCounter++;
      }
      jsonGenerator.copyCurrentEvent(jsonParser); //end array
      wktWriter.write(")"); // end multi line string
    } else if (type.equals("Point")) {
      jsonParser.nextToken();
      lastCoordinate = null;
      wktWriter.write("(");
      processCoordinate(jsonParser, jsonGenerator, wktWriter);
      wktWriter.write(" )");
    } else {
      throw new IllegalStateException("Unsupported type: " + type);
    }
  }

  private Coordinate arrayToCoordinate(List<Double> coordArray) {
    Coordinate coordinate;
    if (coordArray.size() >= 4) {
      coordinate = new CoordinateXYZM(coordArray.get(0), coordArray.get(1), coordArray.get(2), coordArray.get(3));
    } else if (coordArray.size() == 3) {
      coordinate = new Coordinate(coordArray.get(0), coordArray.get(1), coordArray.get(2));
    } else if (coordArray.size() == 2) {
      coordinate = new Coordinate(coordArray.get(0), coordArray.get(1));
    } else {
      throw new IllegalArgumentException("Invalid coordinate: " + coordArray);
    }
    return coordinate;
  }

  private void writeArray(JsonGenerator jsonGenerator, Writer wktWriter, Coordinate coordinate) throws IOException {
    jsonGenerator.writeStartArray();
    List<Double> values = Arrays.asList(coordinate.getX(), coordinate.getY());
    for (Double value : values) {
      jsonGenerator.writeNumber(format.format(value));
      wktWriter.write(new StringBuilder().append(" ").append(format.format(value)).toString());
    }
    jsonGenerator.writeEndArray();
  }

  private void updateStats(double m, double v) {
    distanceM += m;
    count++;
    avgSpeedM = avgSpeedM + (v - avgSpeedM) / count;
  }

  private void processBoundingBox(Coordinate coordinate) {
    Envelope bbox = coordinate.getX() < 0 ? westBoundingBox : eastBoundingBox;
    bbox.expandToInclude(coordinate);
  }

  private void writeBBox(JsonGenerator jsonGenerator) throws IOException {

    double minY;
    double maxY;
    double minX;
    double maxX;

    Envelope boundingBox = new Envelope(westBoundingBox);
    boundingBox.expandToInclude(eastBoundingBox);
    if(boundingBox.getWidth() > 180D) {
      crossedAntimeridian = true;
    }

    if (!crossedAntimeridian) {
      minY = boundingBox.getMinY();
      maxY = boundingBox.getMaxY();
      minX = boundingBox.getMinX();
      maxX = boundingBox.getMaxX();
    } else {
      minY = Math.min(westBoundingBox.getMinY(), eastBoundingBox.getMinY());
      maxY = Math.max(westBoundingBox.getMaxY(), eastBoundingBox.getMaxY());
      minX = eastBoundingBox.getMinX();
      maxX = westBoundingBox.getMaxX();
    }

    // https://tools.ietf.org/html/rfc7946#section-5.3
    if (count == 0 && minX == 0.0 && minY == 0.0 && maxX == -1.0 && maxY == -1.0) { // "bbox":[0.0,0.0,-1.0,-1.0]}
      return; // don't write bbox if no coordinates were specified
    }
    //FIXME
//    else if (boundingBox.contains(new Coordinate(0, 90))) { // north pole
//      doubleArray[0] = -180.0;
//      doubleArray[1] = boundingBox.getMinY();
//      doubleArray[2] = 180.0;
//      doubleArray[3] = 90.0;

    // typical bounding box
//    jsonGenerator.writeFieldName("bbox");
    bbox = new ArrayList<>(4);
    bbox.add(minX);
    bbox.add(minY);
    bbox.add(maxX);
    bbox.add(maxY);

//    jsonGenerator.writeArray(doubleArray, 0, 4);
  }

  private void processCoordinate(JsonParser jsonParser, JsonGenerator jsonGenerator, Writer wktWriter) throws IOException, ValidationException {
    List<Double> coordArray = objectMapper.readValue(jsonParser, LIST_DOUBLE);
    Coordinate coordinate = arrayToCoordinate(coordArray);
    if (lastCoordinate == null) {
      writeArray(jsonGenerator, wktWriter, coordinate);
    } else {
      List<Coordinate> split = splitAm(lastCoordinate, coordinate, geometryFactory);

      if (split.size() == 1) {
        coordinate = split.get(0);
        wktWriter.write(","); //coordinate separator
        writeArray(jsonGenerator, wktWriter, coordinate);
        double m = getDistance(lastCoordinate, coordinate);
        double v = getSpeed(maxAllowedSpeedKnts, lastCoordinate, coordinate, m);
        updateStats(m, v);
      } else if(split.size() == 3) {
        coordinate = split.get(0);
        crossedAntimeridian = true;
        jsonGenerator.writeEndArray();
        jsonGenerator.writeStartArray();
        wktWriter.write("), ("); //coordinate separator // TODO: if split size == 3
        writeArray(jsonGenerator, wktWriter, split.get(2));
        wktWriter.write(","); //coordinate separator
        writeArray(jsonGenerator, wktWriter, coordinate);
        double m = getDistance(split.get(2), coordinate);
        double v = getSpeed(maxAllowedSpeedKnts, lastCoordinate, coordinate, m);
        updateStats(m, v);
      } else if(split.size() == 4) {
        coordinate = split.get(3);
        crossedAntimeridian = true;
        wktWriter.write(","); //coordinate separator
        writeArray(jsonGenerator, wktWriter, split.get(1));
        jsonGenerator.writeEndArray();
        jsonGenerator.writeStartArray();
        wktWriter.write("), ("); //coordinate separator
        writeArray(jsonGenerator, wktWriter, split.get(2));
        wktWriter.write(",");
        writeArray(jsonGenerator, wktWriter, coordinate);
        double m = getDistance(lastCoordinate, split.get(1)) + getDistance(split.get(2), split.get(3));
        double v = getSpeed(maxAllowedSpeedKnts, lastCoordinate, split.get(3), m);
        updateStats(m, v);
      }
    }
    processBoundingBox(coordinate);
    lastCoordinate = coordinate;
  }

  private void processLineString(JsonParser jsonParser, JsonGenerator jsonGenerator, Writer wktWriter) throws IOException, ValidationException {
    jsonGenerator.copyCurrentEvent(jsonParser); //start array
    wktWriter.write("("); //start line string
    lastCoordinate = null;
    while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
      processCoordinate(jsonParser, jsonGenerator, wktWriter);
    }
    jsonGenerator.copyCurrentEvent(jsonParser); //end array
    wktWriter.write(")"); //end line string
  }

  private void copyEverythingElse(JsonParser jsonParser, JsonGenerator jsonGenerator) throws IOException {
    jsonParser.nextToken();
    if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT) {
      copyObject(jsonParser, jsonGenerator);
    } else {
      jsonGenerator.copyCurrentEvent(jsonParser);
    }
  }

  private String verifyFeatureType(JsonParser jsonParser, JsonGenerator jsonGenerator, String... type) throws IOException {
    jsonParser.nextToken();
    jsonGenerator.copyCurrentEvent(jsonParser);
    String value = jsonParser.getText();
    if (!Arrays.asList(type).contains(value)) {
      throw new IllegalArgumentException("Invalid geojson type for AM splitting: " + value);
    }
    return value;
  }
}

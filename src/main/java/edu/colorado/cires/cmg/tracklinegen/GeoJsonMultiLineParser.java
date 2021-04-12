package edu.colorado.cires.cmg.tracklinegen;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoJsonMultiLineParser {
  private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonMultiLineParser.class);
  private static final double METERS_PER_NMILE = 1852D;
  private static final double SECONDS_PER_HOUR = 3600D;
  private static final double KN_PER_MPS = SECONDS_PER_HOUR / METERS_PER_NMILE;

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
  private double avgSpeedM = 0;
  private GeometryProperties properties = null;
  private ArrayList<Double> bbox = null;
  private Coordinate lastCoordinate = null;
  private Coordinate absoluteLastCoordinate = null;

  public GeoJsonMultiLineParser(ObjectMapper objectMapper, int precision, double maxAllowedSpeedKnts) {
    this.objectMapper = objectMapper;
    StringBuilder sb = new StringBuilder("0.");
    for (int i = 1; i <= precision; i++) {
      sb.append("#");
    }
    format = new DecimalFormat(sb.toString(), DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    this.maxAllowedSpeedKnts = maxAllowedSpeedKnts;
  }

  private void updateLastCoordinate(Coordinate coordinate) {
    lastCoordinate = coordinate;
    absoluteLastCoordinate = coordinate;
  }

  private void resetLastCoordinate() {
    lastCoordinate = null;
  }

  private static double mpsToKnots(double metersPerSecond) {
    return metersPerSecond * KN_PER_MPS;
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

  public void parse(JsonParser jsonParser, JsonGenerator jsonGenerator, Writer wktWriter, Map<String, Object> additionalProperties) throws IOException, ValidationException {

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

    GeometryProperties.Builder propertiesBuilder = GeometryProperties.Builder.configure(properties)
        .withDistanceM(distanceM)
        .withAvgSpeedMPS(avgSpeedM);

    if (additionalProperties != null) {
      for(Entry<String, Object> entry : additionalProperties.entrySet()) {
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
    wktWriter.write("MULTILINESTRING ");
    while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
      jsonGenerator.copyCurrentEvent(jsonParser);
      String fieldName = jsonParser.getCurrentName();
      if ("type".equals(fieldName)) {
        verifyFeatureType(jsonParser, jsonGenerator, "MultiLineString");
      } else if ("coordinates".equals(fieldName)) {
        processCoordinates(jsonParser, jsonGenerator, wktWriter);
        writeBBox(jsonGenerator);
      } else {
        copyEverythingElse(jsonParser, jsonGenerator);
      }
    }
    jsonGenerator.copyCurrentEvent(jsonParser); //end object
  }

  private void processCoordinates(JsonParser jsonParser, JsonGenerator jsonGenerator, Writer wktWriter) throws IOException, ValidationException {
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
    jsonGenerator.writeNumber(format.format(coordinate.getX()));
    jsonGenerator.writeNumber(format.format(coordinate.getY()));
    wktWriter.write(new StringBuilder().append(" ").append(format.format(coordinate.getX())).toString());
    wktWriter.write(new StringBuilder().append(" ").append(format.format(coordinate.getY())).toString());
    jsonGenerator.writeEndArray();
  }

  private void writeArray(JsonGenerator jsonGenerator, Writer wktWriter, List<Double> coordinate) throws IOException {
    jsonGenerator.writeStartArray();
    for (Double value : coordinate.subList(0, 2)) {
      jsonGenerator.writeNumber(format.format(value));
      wktWriter.write(new StringBuilder().append(" ").append(format.format(value)).toString());
    }
    jsonGenerator.writeEndArray();
  }

  private double getDistance(Coordinate c1, Coordinate c2) {
    GeodeticCalculator calc = new GeodeticCalculator(DefaultEllipsoid.WGS84);
    calc.setStartingGeographicPoint(c1.getX(), c1.getY());
    calc.setDestinationGeographicPoint(c2.getX(), c2.getY());
    return calc.getOrthodromicDistance();
  }

  private double getSpeed(Coordinate c1, Coordinate c2, double m) throws ValidationException {
    double s = (c2.getZ() - c1.getZ()) / 1000D;
    double metersPerSecond = m / s;

    double knots = mpsToKnots(metersPerSecond);
    if (maxAllowedSpeedKnts > 0 && knots > maxAllowedSpeedKnts) {
      throw new ValidationException(
          String.format("Speed from (%f, %f, %s) to (%f, %f, %s) was %f knots, which exceeded allowed maximum of %f knots",
              c1.getX(), c1.getY(), Instant.ofEpochMilli((long) c1.getZ()),
              c2.getX(), c2.getY(), Instant.ofEpochMilli((long)c2.getZ()),
              knots,
              maxAllowedSpeedKnts
          ));
    }
    return metersPerSecond;
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

    if (!crossedAntimeridian) {
      Envelope boundingBox = new Envelope(westBoundingBox);
      boundingBox.expandToInclude(eastBoundingBox);
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

  private void processLineString(JsonParser jsonParser, JsonGenerator jsonGenerator, Writer wktWriter) throws IOException, ValidationException {
    jsonGenerator.copyCurrentEvent(jsonParser); //start array
    wktWriter.write("("); //start line string
    resetLastCoordinate();
    while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
      List<Double> coordArray = objectMapper.readValue(jsonParser, LIST_DOUBLE);
      Coordinate coordinate = arrayToCoordinate(coordArray);

      if (lastCoordinate == null) {
        writeArray(jsonGenerator, wktWriter, coordArray);
        if (absoluteLastCoordinate != null) {
          List<Coordinate> split = splitAm(coordinate, absoluteLastCoordinate);
          if (split.size() > 1) {
            crossedAntimeridian = true;
          }
        }
      } else {
        List<Coordinate> split = splitAm(coordinate, lastCoordinate);
        if (split.size() == 1) {
          wktWriter.write(","); //coordinate separator
          writeArray(jsonGenerator, wktWriter, coordArray);
          double m = getDistance(lastCoordinate, split.get(0));
          double v = getSpeed(lastCoordinate, split.get(0), m);
          updateStats(m, v);
        } else {
          crossedAntimeridian = true;
          wktWriter.write(","); //coordinate separator
          writeArray(jsonGenerator, wktWriter, split.get(1));
          jsonGenerator.writeEndArray();
          jsonGenerator.writeStartArray();
          wktWriter.write("), ("); //coordinate separator // TODO: if split size == 3
          writeArray(jsonGenerator, wktWriter, split.get(2));
          // TODO - if split size == 3 and coordinate is the very last coordinate will result in a lineString with a single point
          if (split.size() == 4) {
            wktWriter.write(","); //coordinate separator
            writeArray(jsonGenerator, wktWriter, split.get(3));
            double m = getDistance(split.get(0), split.get(1));
            m += getDistance(split.get(2), split.get(3));
            double v = getSpeed(lastCoordinate, split.get(3), m);
            updateStats(m, v);
          }
        }
      }
      processBoundingBox(coordinate);
      updateLastCoordinate(coordinate);
    }
    jsonGenerator.copyCurrentEvent(jsonParser); //end array
    wktWriter.write(")"); //end line string
  }

  private List<Coordinate> splitAm(Coordinate coordinate, Coordinate last) {
    double x = coordinate.getX();
    double y = coordinate.getY();
    if (x == 180D || x == -180D) {
      if (last.getX() == 180D || last.getX() == -180D) {
        return Collections.singletonList(new Coordinate(last.getX(), y, coordinate.getZ()));
      }

      double sign = Math.signum(last.getX());
      return Arrays.asList(
          last,
          new Coordinate(180D * sign, y),
          new Coordinate(180D * sign * -1, y)
      );
    }

    List<Coordinate> split;
    if ((coordinate.getX() < 0 && last.getX() > 0) || (coordinate.getX() > 0 && last.getX() < 0)) {
      LineString lineString = geometryFactory.createLineString(new Coordinate[]{last, coordinate});
      Geometry geometry = new JtsGeometry(lineString, JtsSpatialContext.GEO, true, true).getGeom();
      if (geometry instanceof LineString) {
        return Collections.singletonList(coordinate);
      } else if (geometry instanceof GeometryCollection) {
        split = geometryParse(coordinate, last, (GeometryCollection) geometry);
      } else {
        throw new IllegalStateException(
            String.format("An error occurred splitting AM, type: %s from coordinates (%f, %f, %s) to (%f, %f, %s)",
                geometry.toString(),
                coordinate.getX(), coordinate.getY(), Instant.ofEpochMilli((long)coordinate.getZ()),
                last.getX(), last.getY(), Instant.ofEpochMilli((long)last.getZ())
            ));
      }
    } else {
      split = Collections.singletonList(coordinate);
    }
    return split;
  }

  private void copyEverythingElse(JsonParser jsonParser, JsonGenerator jsonGenerator) throws IOException {
    jsonParser.nextToken();
    if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT) {
      copyObject(jsonParser, jsonGenerator);
    } else {
      jsonGenerator.copyCurrentEvent(jsonParser);
    }
  }

  private void verifyFeatureType(JsonParser jsonParser, JsonGenerator jsonGenerator, String type) throws IOException {
    jsonParser.nextToken();
    jsonGenerator.copyCurrentEvent(jsonParser);
    String value = jsonParser.getText();
    if (!type.equals(value)) {
      throw new IllegalArgumentException("Invalid geojson type for AM splitting: " + value);
    }
  }

  private Coordinate resolveCoordinate(LineString lineString, Coordinate last){
    if (lineString.getCoordinateN(0).equals(last)){
      return lineString.getCoordinateN(1);
    }
    return lineString.getCoordinateN(0);
  }

  private List<Coordinate> geometryParse(Coordinate coordinate, Coordinate last, GeometryCollection geometry){
    Geometry l1 = geometry.getGeometryN(0);
    Geometry l2 = geometry.getGeometryN(1);

    if (l1 instanceof LineString && l2 instanceof LineString) {
      return geometryParse(coordinate, last, (LineString) l1, (LineString) l2, geometry);
    } else if (l1 instanceof LineString && l2 instanceof Point){
      return Collections.singletonList(resolveCoordinate((LineString) l1, last));
    } else if (l2 instanceof LineString && l1 instanceof Point){
      return Collections.singletonList(resolveCoordinate((LineString) l2, last));
    } else {
      throw new IllegalStateException(
          String.format("An error occurred splitting AM, type: %s from coordinates (%f, %f, %s) to (%f, %f, %s)",
              geometry.toString(),
              coordinate.getX(), coordinate.getY(), Instant.ofEpochMilli((long)coordinate.getZ()),
              last.getX(), last.getY(), Instant.ofEpochMilli((long)last.getZ())
          ));
    }
  }

  private List<Coordinate> geometryParse(Coordinate coordinate, Coordinate last, LineString l1, LineString l2, Geometry geometry){
    List<Coordinate> split = new ArrayList<>(4);
    if (l1.getCoordinateN(0).equals(last) || l2.getCoordinateN(1).equals(coordinate)) {
      split.add(l1.getCoordinateN(0));
      split.add(l1.getCoordinateN(1));
      split.add(l2.getCoordinateN(0));
      split.add(l2.getCoordinateN(1));
    } else if (l2.getCoordinateN(0).equals(last) || l1.getCoordinateN(1).equals(coordinate)) {
      split.add(l2.getCoordinateN(0));
      split.add(l2.getCoordinateN(1));
      split.add(l1.getCoordinateN(0));
      split.add(l1.getCoordinateN(1));
    } else {
      throw new IllegalStateException("Unable to determine AM split order: " + geometry +
          " coordinate: " + coordinate + " last: " + last + " L1: " + l1 + " l2: " + l2);
    }
    return split;
  }
}

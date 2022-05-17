package edu.colorado.cires.cmg.tracklinegen.test;

import static edu.colorado.cires.cmg.tracklinegen.JsonPropertiesUtils.assertJsonEquivalent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.colorado.cires.cmg.iostream.Pipe;
import edu.colorado.cires.cmg.tracklinegen.BaseRowListener;
import edu.colorado.cires.cmg.tracklinegen.DataRow;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineProcessor;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineWriter;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import edu.colorado.cires.cmg.tracklinegen.TracklineProcessor;
import edu.colorado.cires.cmg.tracklinegen.ValidationException;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FnvSplittingTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(FnvSplittingTest.class);

  private static final class MultiFileReader implements Closeable {

    private final List<Path> paths;
    private int nextIndex;
    private BufferedReader currentReader;

    public MultiFileReader(List<Path> paths) {
      this.paths = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(paths)));
    }

    public String readLine() {
      if (currentReader == null) {
        setCurrentReader();
      }
      String temp = getLine();
      while (temp == null && nextIndex < paths.size()) {
        close();
        setCurrentReader();
        temp = getLine();
      }
      return temp;
    }

    private String getLine() {
      try {
        return currentReader.readLine();
      } catch (IOException e) {
        String msg = "Unable to read line";
        LOGGER.error(msg);
        throw new RuntimeException(msg, e);
      }
    }

    private void setCurrentReader() {
      try {
        Path path = paths.get(nextIndex);
        LOGGER.debug("Reading: {}", path);
        currentReader = Files.newBufferedReader(path);
        nextIndex++;
      } catch (Exception e) {
        close();
        String msg = "Unable to open file";
        LOGGER.error("Unable to open file", e);
        throw new RuntimeException(msg, e);
      }
    }

    @Override
    public void close() {
      if (currentReader != null) {
        try {
          currentReader.close();
        } catch (Exception e) {
          LOGGER.warn("Unable to close reader", e);
        }
      }
    }
  }

  private static class FnvDataRow implements DataRow {

    private final Instant timestamp;
    private final double lon;
    private final double lat;

    public FnvDataRow(Instant timestamp, double lon, double lat) {
      this.timestamp = Objects.requireNonNull(timestamp);
      this.lon = lon;
      this.lat = lat;
    }

    @Override
    public Instant getTimestamp() {
      return timestamp;
    }

    @Override
    public Double getLon() {
      return lon;
    }

    @Override
    public Double getLat() {
      return lat;
    }
  }

  private static class FnvReaderIterator implements Iterator<FnvDataRow> {


    private final MultiFileReader reader;
    private final Function<String, FnvDataRow> parseRow;
    private boolean fnvDataRowWasRead = true;
    private FnvDataRow fnvDataRow;

    public FnvReaderIterator(MultiFileReader reader, Function<String, FnvDataRow> parseRow) {
      this.reader = reader;
      this.parseRow = parseRow;
    }

    private void parseNextValid() {
      FnvDataRow next = null;
      do {
        String nextLine = reader.readLine();
        if (nextLine != null) {
          next = parseRow.apply(nextLine);
        }
      } while (next != null && !isValid(next));
      fnvDataRow = next;
      fnvDataRowWasRead = false;
    }

    private boolean isValid(FnvDataRow next) {
      if (fnvDataRow == null) {
        return true;
      }
      Instant lastTime = fnvDataRow.getTimestamp();
      Instant thisTime = next.getTimestamp();
      if (thisTime.isBefore(lastTime)) {
        LOGGER.debug("Timestamps are out of order: {} : {}", lastTime, thisTime);
        return false;
      }
      return true;
    }

    @Override
    public boolean hasNext() {
      if (fnvDataRowWasRead) {
        parseNextValid();
      }
      return fnvDataRow != null;
    }

    @Override
    public FnvDataRow next() {
      if (hasNext()) {
        fnvDataRowWasRead = true;
        return fnvDataRow;
      }
      throw new IllegalStateException("End of file");
    }
  }

  private static class FnvTracklineProcessor extends TracklineProcessor<FnvTracklineContext, FnvDataRow, FnvRowListener> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss.SSSSSS");

    public static final Function<String, FnvDataRow> HANDLER = line -> {
      String[] tokens = line.split("\\t");
      double epochSeconds = Double.parseDouble(tokens[1]);
      long epochMillis = (long) (epochSeconds * 1000D);
      Instant timestamp = Instant.ofEpochMilli(epochMillis);
//    Instant timestamp = LocalDateTime.parse(tokens[0], DATE_FORMAT).toInstant(ZoneOffset.UTC);
      return new FnvDataRow(timestamp, Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3]));
    };

    private final long msSplit;
    private final GeometrySimplifier geometrySimplifier;
    private final int batchSize;
    private final List<Path> fnvFiles;
    private final int precision;
    private final ObjectMapper objectMapper;
    private final OutputStream out;
    private final GeometryFactory geometryFactory;

    public FnvTracklineProcessor(long msSplit, GeometrySimplifier geometrySimplifier, int batchSize, List<Path> fnvFiles, int precision,
        ObjectMapper objectMapper, OutputStream out, GeometryFactory geometryFactory) {
      this.msSplit = msSplit;
      this.geometrySimplifier = geometrySimplifier;
      this.batchSize = batchSize;
      this.fnvFiles = fnvFiles;
      this.precision = precision;
      this.objectMapper = objectMapper;
      this.out = out;
      this.geometryFactory = geometryFactory;
    }

    @Override
    protected Iterator<FnvDataRow> getRows(FnvTracklineContext context) {
      return new FnvReaderIterator(context.getReader(), HANDLER);
    }

    @Override
    protected List<FnvRowListener> createRowListeners(FnvTracklineContext context) {
      return Collections.singletonList(
          new FnvRowListener(msSplit, geometrySimplifier, context.getLineWriter(), batchSize, geometryFactory, precision));
    }

    @Override
    protected FnvTracklineContext createProcessingContext() {
      return new FnvTracklineContext(fnvFiles, precision, objectMapper, out);
    }
  }

  private static class FnvTracklineContext implements Closeable {

    private final JsonGenerator jsonGenerator;
    private final GeoJsonMultiLineWriter lineWriter;
    private final MultiFileReader reader;

    public FnvTracklineContext(List<Path> fnvFiles, int precision, ObjectMapper objectMapper, OutputStream out) {
      try {
        jsonGenerator = objectMapper.getFactory().createGenerator(out);
      } catch (IOException e) {
        throw new RuntimeException("Unable to create JSON generator", e);
      }
      lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, precision);
      reader = new MultiFileReader(fnvFiles);
    }

    public GeoJsonMultiLineWriter getLineWriter() {
      return lineWriter;
    }

    public MultiFileReader getReader() {
      return reader;
    }

    @Override
    public void close() throws IOException {
      try {
        reader.close();
      } catch (Exception e) {
        LOGGER.warn("Unable to close reader", e);
      }
      try {
        jsonGenerator.close();
      } catch (Exception e) {
        LOGGER.warn("Unable to close JSON generator", e);
      }
    }
  }

  private static class FnvRowListener extends BaseRowListener<FnvDataRow> {

    public FnvRowListener(long msSplit, GeometrySimplifier geometrySimplifier, GeoJsonMultiLineWriter lineWriter, int batchSize,
        GeometryFactory geometryFactory, int precision) {
      super(msSplit, geometrySimplifier, lineWriter, batchSize, x -> true, 0, geometryFactory, precision);
    }
  }

  private static final int SRID = 8307;
  private static final double simplificationTolerance = 0.0001;
  private static final long splitGeometryMs = 900000L;
  private static final int batchSize = 10000;
  private static final int geoJsonPrecision = 5;
  private static final double maxAllowedSpeedKnts = 60D;
  private static final ObjectMapper objectMapper = objectMapper();
  private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);


  public static ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    return objectMapper;
  }

  public static void generateGeometryFiles(List<Path> fnvFiles, Path geoJsonPath, Path wktPath) throws IOException {

    Files.createDirectories(geoJsonPath.getParent());
    Files.createDirectories(wktPath.getParent());

    Pipe.pipe((out) -> {
      FnvTracklineProcessor phase1 = new FnvTracklineProcessor(splitGeometryMs, new GeometrySimplifier(simplificationTolerance), batchSize, fnvFiles,
          geoJsonPrecision, objectMapper, out, geometryFactory);
      try {
        phase1.process();
      } catch (IOException e) {
        LOGGER.error("Unable to process geometry data", e);
        throw new RuntimeException("Unable to process geometry data", e);
      }
    }, (in) -> {
      try (OutputStream gjOut = Files.newOutputStream(geoJsonPath); OutputStream wktOut = Files.newOutputStream(wktPath)) {
        GeoJsonMultiLineProcessor phase2 = new GeoJsonMultiLineProcessor(objectMapper, geoJsonPrecision, maxAllowedSpeedKnts);
        phase2.process(in, gjOut, wktOut);
      } catch (ValidationException | IOException e) {
        LOGGER.error("Unable write geojson and wkt data", e);
        throw new RuntimeException("Unable write geojson and wkt data", e);
      }
    });

  }

  @Test
  public void test() throws Exception {
    List<Path> fnvFiles = Arrays.asList(
        Paths.get("src/test/resources/fnv/001_0553.HSX.mb201.fnv"),
        Paths.get("src/test/resources/fnv/047_0706.HSX.mb201.fnv")
//        Paths.get("src/test/resources/fnv/050_0737.HSX.mb201.fnv"),
//        Paths.get("src/test/resources/fnv/048_0807.HSX.mb201.fnv"),
//        Paths.get("src/test/resources/fnv/049_0838.HSX.mb201.fnv")
    );
    Path geoJsonPath = Paths.get("target/fnv/fnv.json");
    Path wktPath = Paths.get("target/fnv/fnv.wkt");
    FileUtils.deleteQuietly(geoJsonPath.toFile());
    FileUtils.deleteQuietly(wktPath.toFile());
    generateGeometryFiles(fnvFiles, geoJsonPath, wktPath);

    JsonNode expected = objectMapper.readTree(new File("src/test/resources/fnv/expected1.json"));
    JsonNode actual = objectMapper.readTree(geoJsonPath.toFile());
    assertJsonEquivalent(expected, actual, 0.00001);
  }

  @Test
  public void testDrunkAmWondering() throws Exception {
    List<Path> fnvFiles;
    try(Stream<Path> stream = Files.list(Paths.get("src/test/resources/fnv_am/KM0625"))) {
      fnvFiles = stream.filter(f -> f.toString().endsWith(".mb56.fnv")).sorted().collect(Collectors.toList());
    }
    Path geoJsonPath = Paths.get("target/fnv/fnv.json");
    Path wktPath = Paths.get("target/fnv/fnv.wkt");
    FileUtils.deleteQuietly(geoJsonPath.toFile());
    FileUtils.deleteQuietly(wktPath.toFile());
    generateGeometryFiles(fnvFiles, geoJsonPath, wktPath);

    JsonNode expected = objectMapper.readTree(new File("src/test/resources/fnv/expected2.json"));
    JsonNode actual = objectMapper.readTree(geoJsonPath.toFile());
    assertJsonEquivalent(expected, actual, 0.00001);
  }

}

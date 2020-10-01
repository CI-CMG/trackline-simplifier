package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TracklineProcessorTest {
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void test() throws Exception {

    final int geoJsonPrecision = 5;
    final double simplificationTolerance = 0.0001;
    final int simplifierBatchSize = 3000;
    final int msSplit = 3600000;
    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(0.0001);
    ObjectMapper objectMapper = new ObjectMapper();
    Path fnvFile = Paths.get("src/test/resources/phase1/test1/data.csv");
    Path gsf = Paths.get("target/test-classes/geoSimplfied.json");

    GeoSimplifierProcessor tracklineProcessor = new GeoSimplifierProcessor(geoJsonPrecision, msSplit, geometrySimplifier, simplifierBatchSize, fnvFile,
    objectMapper, gsf);

    tracklineProcessor.process();

//    Geometry1Processor geometry1Processor = new Geometry1Processor(msSplit, geoJsonPrecision,
//        simplificationTolerance, simplifierBatchSize, objectMapper, outFile, inFile);
  }

}

//
//    long msSplit = 3600000;
//    GeometrySimplifier geometrySimplifier = new GeometrySimplifier(0.0001);
//    int batchSize = 3000;
//    Path fnvFile = Paths.get("src/test/resources/trackline/plat/crz/0000_20130826_043104_EX1305_MB.all.mb58.fnv");
//    int precision = 5;
//    ObjectMapper objectMapper = new ObjectMapper();
//    Path gsf = Paths.get("target/0000_20130826_043104_EX1305_MB_geoSimplfied.json");
//
//    // Geometry Simplifier
//    MbTracklineProcessor tracklineProcessor = new MbTracklineProcessor(msSplit, geometrySimplifier, batchSize, fnvFile, precision,
//    objectMapper, gsf);
//    tracklineProcessor.process();
//
//
//    // GeoJson and MKL
//    Path geoJsonFile = Paths.get("target/0000_20130826_043104_EX1305_MB.json");
//    Path wktFile = Paths.get("target/0000_20130826_043104_EX1305_MB.wkt");
//    ObjectMapper objectMapper1 = new ObjectMapper();
//    int geoJsonPrecision = 5;
//    double maxAllowedSpeedKnts = 20;
//    MbGeoJsonProcessor geoJsonProcessor = new MbGeoJsonProcessor(gsf, geoJsonFile, wktFile, objectMapper1, geoJsonPrecision, maxAllowedSpeedKnts);
//    geoJsonProcessor.process();
//
//  }

//  BufferedReader reader = new BufferedReader(new FileReader("src/test/resources/phase1/test1/data.csv"));
//  String line = reader.readLine();

//  FileWriter myWriter = new FileWriter(String.valueOf(gsf));
//    myWriter.write("Files in Java might be tricky, but it is fun enough!");
//        myWriter.close();
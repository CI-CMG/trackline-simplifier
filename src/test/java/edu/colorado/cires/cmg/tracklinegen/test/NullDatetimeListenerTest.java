package edu.colorado.cires.cmg.tracklinegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineWriter;
import edu.colorado.cires.cmg.tracklinegen.GeometrySimplifier;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.GeoDataRow;
import edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.GsBaseRowListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public class NullDatetimeListenerTest {

    private final int maxCount = 0;
    private final int geoJsonPrecision = 4;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    final double tol = 0.0001;
    private final GeometrySimplifier geometrySimplifier = new GeometrySimplifier(tol);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private double maxAllowedSpeedKnts = 0D;

    @Test
    public void testNullDatetime() throws Exception {
        List<GeoDataRow> rows = Arrays.asList(
                new GeoDataRow(null,-157.88034, 21.31367),
                new GeoDataRow(null,-157.89551, 21.3075),
                new GeoDataRow(null,-157.89037, 21.30076),
                new GeoDataRow(null,-157.89187, 21.27442)
        );

        final long msSplit = 1000L * 2L;
        final int batchSize = 5000;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JsonGenerator jsonGenerator = objectMapper.getFactory().createGenerator(out)) {
            GeoJsonMultiLineWriter lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, geoJsonPrecision);
            GsBaseRowListener listener = new GsBaseRowListener(
                    msSplit,
                    geometrySimplifier,
                    lineWriter,
                    batchSize,
                    maxCount,
                    geometryFactory,
                    geoJsonPrecision,
                    dataRow -> true,
                    maxAllowedSpeedKnts
            );
            listener.start();
            rows.forEach(listener::processRow);
            listener.finish();
        }
        JsonNode expected;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("null-datetime.json")) {
            expected = objectMapper.readTree(in);
        }

        JsonNode geoJson = objectMapper.readTree(out.toByteArray());

        assertEquals(expected, geoJson);

    }
}

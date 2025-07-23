package edu.colorado.cires.cmg.tracklinegen.test;

import static edu.colorado.cires.cmg.tracklinegen.JsonPropertiesUtils.assertJsonEquivalent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineProcessor;
import edu.colorado.cires.cmg.tracklinegen.ValidationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

class DuplicateTimestampTest {

  private final Path testPath = Paths.get("src/test/resources/duplicate-timestamps");

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testSettingEnabled() throws IOException, ValidationException {
    GeoJsonMultiLineProcessor processor = new GeoJsonMultiLineProcessor(new ObjectMapper(), 4, 60D,
      true);
    try (
      InputStream inputStream = Files.newInputStream(testPath.resolve("input.json"));
      OutputStream geoJsonOutputStream = new ByteArrayOutputStream();
      OutputStream wktOutputStream = new ByteArrayOutputStream()

    ) {
      processor.process(inputStream, geoJsonOutputStream, wktOutputStream);

      assertJsonEquivalent(objectMapper.readTree(
        FileUtils.readFileToString(testPath.resolve("expected.json").toFile(),
          StandardCharsets.UTF_8)), objectMapper.readTree(geoJsonOutputStream.toString()), 4);
      assertEquals(FileUtils.readFileToString(testPath.resolve("expected.wkt").toFile(),
        StandardCharsets.UTF_8), wktOutputStream.toString());
    }
  }

  @Test
  void testSettingDisabled() throws IOException {
    GeoJsonMultiLineProcessor processor = new GeoJsonMultiLineProcessor(new ObjectMapper(), 4,
      60D); // allowDuplicateTimestamps false by default
    try (
      InputStream inputStream = Files.newInputStream(testPath.resolve("input.json"));
      OutputStream geoJsonOutputStream = new ByteArrayOutputStream();
      OutputStream wktOutputStream = new ByteArrayOutputStream()

    ) {
      assertThrows(ValidationException.class,
        () -> processor.process(inputStream, geoJsonOutputStream, wktOutputStream));
    }
  }
}

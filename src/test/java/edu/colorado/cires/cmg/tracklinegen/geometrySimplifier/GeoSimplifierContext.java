package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import edu.colorado.cires.cmg.tracklinegen.GeoJsonMultiLineWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GeoSimplifierContext implements Closeable{
  private BufferedReader reader;
  private JsonGenerator jsonGenerator;
  private GeoJsonMultiLineWriter lineWriter;

  public GeoSimplifierContext(Path fnvFile, int precision, ObjectMapper objectMapper, Path jsonFile){

    try {
      reader = Files.newBufferedReader(fnvFile);
      jsonGenerator = objectMapper.getFactory().createGenerator(Files.newOutputStream(jsonFile));
    } catch (IOException e) {
      e.printStackTrace();
    }
    lineWriter = new GeoJsonMultiLineWriter(jsonGenerator, precision);
  }

  public BufferedReader getReader() {
    return reader;
  }

  public GeoJsonMultiLineWriter getLineWriter() {
    return lineWriter;
  }

  @Override
  public void close() throws IOException {
    jsonGenerator.close();
    reader.close();
  }
}
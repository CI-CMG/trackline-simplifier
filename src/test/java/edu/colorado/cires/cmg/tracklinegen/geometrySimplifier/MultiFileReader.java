package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MultiFileReader implements Closeable {
  private final List<Path> paths;
  private BufferedReader currentReader;
  private int currentIndex = 0;

  public MultiFileReader(List<Path> paths) {
    this.paths = paths;
  }

  public String readLine() throws IOException{
    try {
      if (currentReader == null){
        currentReader = Files.newBufferedReader(paths.get(currentIndex));
      }
      String temp = currentReader.readLine();
      while (temp == null && currentIndex < paths.size()) {
        currentReader.close();
        currentReader = Files.newBufferedReader(paths.get(currentIndex));
        currentIndex += 1;
      }
      return temp;
    } catch (IOException|RuntimeException e){
        if (currentReader != null){
          currentReader.close();
        }
        throw e;
    }

  }

  @Override
  public void close() throws IOException {
    if (currentReader != null){
      currentReader.close();
    }
  }
}

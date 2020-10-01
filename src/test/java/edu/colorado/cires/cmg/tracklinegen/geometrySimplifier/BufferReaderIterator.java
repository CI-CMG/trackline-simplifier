package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

public class BufferReaderIterator implements Iterator {

  private BufferedReader reader;
  private String line = null;

  public BufferReaderIterator(BufferedReader reader) {
    this.reader = reader;
  }

  @Override
  public boolean hasNext() {
    if (line != null){
      return true;
    }
    String nextline = null;
    try {
      nextline = reader.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (nextline == null){
      return false;
    }
    line = nextline;
    return true;
  }

  private GeoDataRow apply(String line){
    String [] tokens = line.split("\t");
    LocalDateTime dateTime = LocalDateTime.parse(tokens[0], DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss.SSSSSS"));
    Instant timeStamp = dateTime.toInstant(ZoneOffset.UTC);
    return new GeoDataRow(timeStamp, Double.valueOf(tokens[2]), Double.valueOf(tokens[3]));
  }

  @Override
  public Object next() {
    if (hasNext()){
      GeoDataRow dataRow = apply(line);
      line = null;
      return dataRow;
    }
    return null;
  }
}

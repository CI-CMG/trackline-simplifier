package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier;

import edu.colorado.cires.cmg.tracklinegen.DataRow;
import java.time.Instant;

public class GeoDataRow implements DataRow {
  private Instant timeStamp;
  private Double lon1;
  private Double lat1;
  private Double bathyTime;

  public GeoDataRow(Instant timeStamp, Double lon1, Double lat1) {
    this.timeStamp = timeStamp;
    this.lon1 = lon1;
    this.lat1 = lat1;
  }

  public GeoDataRow(Instant timeStamp, Double lon1, Double lat1, Double bathyTime) {
    this.timeStamp = timeStamp;
    this.lon1 = lon1;
    this.lat1 = lat1;
    this.bathyTime = bathyTime;
  }

  public Double getBathyTime() {
    return bathyTime;
  }

  public void setBathyTime(Double bathyTime) {
    this.bathyTime = bathyTime;
  }

  @Override
  public Instant getTimestamp() {
    return timeStamp;
  }

  @Override
  public Double getLon() {
    return lon1;
  }

  @Override
  public Double getLat() {
    return lat1;
  }
}

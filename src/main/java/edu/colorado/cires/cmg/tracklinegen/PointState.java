package edu.colorado.cires.cmg.tracklinegen;

import org.locationtech.jts.geom.Point;

public class PointState {

  private final Point point;
  private boolean target;
  private boolean confirmed;
  private boolean startSegment;

  public PointState(Point point, boolean target) {
    this.point = point;
    this.target = target;
    this.confirmed = target;
  }

  public boolean isStartSegment() {
    return startSegment;
  }

  public void setStartSegment(boolean startSegment) {
    this.startSegment = startSegment;
  }

  public boolean isConfirmed() {
    return confirmed;
  }

  public void setConfirmed(boolean confirmed) {
    this.confirmed = confirmed;
  }

  public void setTarget(boolean target) {
    this.target = target;
  }

  public boolean isTarget() {
    return target;
  }

  public Point getPoint() {
    return point;
  }

}

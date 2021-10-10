package edu.colorado.cires.cmg.tracklinegen;

import org.locationtech.jts.geom.Point;

public class PointState {

  private final Point point;
  private boolean target;
  private boolean confirmed;
  private int index;

  public PointState(Point point, boolean target) {
    this.point = point;
    this.target = target;
    this.confirmed = target;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
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

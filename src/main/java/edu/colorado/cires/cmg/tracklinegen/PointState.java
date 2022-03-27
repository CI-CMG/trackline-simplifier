package edu.colorado.cires.cmg.tracklinegen;

import org.locationtech.jts.geom.Point;

public class PointState {

  private final Point point;
  private boolean target;
  private boolean simplified;
  private int index;

  public PointState(Point point, boolean target) {
    this.point = point;
    this.target = target;
  }

  public boolean isSimplified() {
    return simplified;
  }

  public void setSimplified(boolean simplified) {
    this.simplified = simplified;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public boolean isTarget() {
    return target;
  }

  public Point getPoint() {
    return point;
  }

  @Override
  public String toString() {
    return "PointState{" +
        "point=" + point +
        ", target=" + target +
        ", simplified=" + simplified +
        ", index=" + index +
        '}';
  }
}

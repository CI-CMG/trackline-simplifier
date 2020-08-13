package edu.colorado.cires.cmg.tracklinegen;

import java.util.List;

public class RowProcessor <D extends DataRow, L extends RowListener<D>> {


  private final List<L> listeners;

  public RowProcessor(List<L> listeners) {
    this.listeners = listeners;
  }

  public void start() {
    listeners.forEach(listener -> listener.start());
  }

  public void processRow(D row) {
    listeners.forEach(listener -> listener.processRow(row));
  }

  public void finish() {
    listeners.forEach(listener -> listener.finish());
  }
  
}

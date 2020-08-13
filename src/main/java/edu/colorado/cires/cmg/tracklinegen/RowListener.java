package edu.colorado.cires.cmg.tracklinegen;

public interface RowListener<T extends DataRow> {

  void start();
  void processRow(T row);
  void finish();

}

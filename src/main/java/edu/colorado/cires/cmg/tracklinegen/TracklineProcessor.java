package edu.colorado.cires.cmg.tracklinegen;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public abstract class TracklineProcessor<C extends Closeable, D extends DataRow, L extends RowListener> {

  public void process() throws IOException {
    try (
        C context = createProcessingContext()
    ) {
      Iterator<D> iterator = getRows(context);
      RowProcessor rowProcessor = new RowProcessor(createRowListeners(context));
      rowProcessor.start();
      while (iterator.hasNext()){
        D row = iterator.next();
        rowProcessor.processRow(row);
      }
      rowProcessor.finish();
    }
  }

  protected abstract Iterator<D> getRows(C context);

  protected abstract List<L> createRowListeners(C context);

  protected abstract C createProcessingContext();
}

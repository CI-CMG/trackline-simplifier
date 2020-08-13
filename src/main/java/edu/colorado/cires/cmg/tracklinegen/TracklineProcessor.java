package edu.colorado.cires.cmg.tracklinegen;

import com.fasterxml.jackson.core.JsonParser;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public abstract class TracklineProcessor <C extends Closeable, D extends DataRow, L extends RowListener>{
  public void process() throws IOException {
    try (
        C context = createProcessingContext()
    ) {
      Stream<D> rowStream = getRowStream(context);
      RowProcessor rowProcessor = new RowProcessor(createRowListeners(context));
      rowProcessor.start();
      rowStream.forEach(rowProcessor::processRow);
      rowProcessor.finish();
    }

  }

  protected abstract Stream<D> getRowStream(C context);
  protected abstract List<L> createRowListeners(C context);
  protected abstract C createProcessingContext();
}

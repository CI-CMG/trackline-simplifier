package edu.colorado.cires.cmg.tracklinegen;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public final class GeometryFactoryFactory {

  public static GeometryFactory create() {
    return new GeometryFactory(new PrecisionModel(), 4326);
  }

  private GeometryFactoryFactory() {

  }
}

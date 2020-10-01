package edu.colorado.cires.cmg.tracklinegen.geometrySimplifier.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DoubleSerializer extends StdSerializer<Double> {

  public DoubleSerializer() {
    this(null);
  }

  public DoubleSerializer(Class<Double> t) {
    super(t);
  }

  @Override
  public void serialize(Double value, JsonGenerator gen, SerializerProvider provider) throws IOException {
    if (value == null) {
      gen.writeNull();
    } else {
      DecimalFormat format = new DecimalFormat("0.#################", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
      gen.writeNumber(format.format(value));
    }
  }
}

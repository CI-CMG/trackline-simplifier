package edu.colorado.cires.cmg.tracklinegen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.FloatNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class JsonPropertiesUtils {

  public static void assertJsonEquivalent(JsonNode expected, JsonNode actual, double precision) {
    if (expected instanceof ObjectNode) {
      if (actual instanceof ObjectNode) {
        Set<String> expectedFields = new HashSet<>();
        for (Iterator<String> it = ((ObjectNode) expected).fieldNames(); it.hasNext(); ) {
          String field = it.next();
          expectedFields.add(field);
        }
        Set<String> actualFields = new HashSet<>();
        for (Iterator<String> it = ((ObjectNode) actual).fieldNames(); it.hasNext(); ) {
          String field = it.next();
          actualFields.add(field);
        }
        if(expectedFields.equals(actualFields)) {
          expectedFields.forEach(field -> {
            JsonNode expectedChild = expected.get(field);
            JsonNode actualChild = actual.get(field);
            assertJsonEquivalent(expectedChild, actualChild, precision);
          });
        } else {
          assertEquals(expected, actual);
        }
      } else {
        assertEquals(expected, actual);
      }
    } else if (expected instanceof ArrayNode) {
      if (actual instanceof ArrayNode && expected.size() == actual.size()) {
        for (int i = 0; i < expected.size(); i++) {
          JsonNode expectedChild = expected.get(i);
          JsonNode actualChild = actual.get(i);
          assertJsonEquivalent(expectedChild, actualChild, precision);
        }
      } else {
        assertEquals(expected, actual);
      }
    } else if (expected instanceof DoubleNode || expected instanceof FloatNode) {
      if (actual instanceof DoubleNode || expected instanceof FloatNode) {
        double d = expected.doubleValue() - actual.doubleValue();
        assertEquals(expected.doubleValue(), actual.doubleValue(), precision);
      } else {
        assertEquals(expected, actual);
      }
    } else {
      assertEquals(expected, actual);
    }
  }
}

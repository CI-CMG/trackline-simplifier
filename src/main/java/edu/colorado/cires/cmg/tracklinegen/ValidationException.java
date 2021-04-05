package edu.colorado.cires.cmg.tracklinegen;

import java.util.HashMap;
import java.util.Map;

public class ValidationException extends Exception {
  private final Map<String, Object> properties = new HashMap<>();

  public ValidationException(String message) {
    super(message);
  }

  public ValidationException(String message, Map<String, Object> properties) {
    super(message);
    this.properties.putAll(properties);
  }
  public Map<String, Object> getProperties() {
    return properties;
  }
}

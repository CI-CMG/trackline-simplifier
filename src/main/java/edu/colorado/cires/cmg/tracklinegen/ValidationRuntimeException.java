package edu.colorado.cires.cmg.tracklinegen;

public class ValidationRuntimeException extends RuntimeException {

  public ValidationRuntimeException(ValidationException e) {
    super(e.getMessage());
  }
}

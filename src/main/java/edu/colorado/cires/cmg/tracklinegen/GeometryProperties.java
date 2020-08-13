package edu.colorado.cires.cmg.tracklinegen;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(builder = GeometryProperties.Builder.class)
public class GeometryProperties {

  private final Double distanceM;
  private final Double avgSpeedMPS;
  private final Long unsimplifiedPointCount;
  private final Long simplifiedPointCount;
  private final Long targetPointCount;

  private final Map<String, Object> otherFields;

  private GeometryProperties(Builder builder) {
    distanceM = builder.distanceM;
    avgSpeedMPS = builder.avgSpeedMPS;
    unsimplifiedPointCount = builder.unsimplifiedPointCount;
    simplifiedPointCount = builder.simplifiedPointCount;
    targetPointCount = builder.targetPointCount;
    otherFields = builder.otherFields;
  }

  public Double getDistanceM() {
    return distanceM;
  }

  public Double getAvgSpeedMPS() {
    return avgSpeedMPS;
  }

  public Long getUnsimplifiedPointCount() {
    return unsimplifiedPointCount;
  }

  public Long getSimplifiedPointCount() {
    return simplifiedPointCount;
  }

  public Long getTargetPointCount() {
    return targetPointCount;
  }

  @JsonAnyGetter
  private Map<String, Object> getOtherFields() {
    return otherFields;
  }


  public static class Builder {

    private Double distanceM;
    private Double avgSpeedMPS;
    private Long unsimplifiedPointCount;
    private Long simplifiedPointCount;
    private Long targetPointCount;
    private Map<String, Object> otherFields = new HashMap<>();

    public static Builder configure() {
      return new Builder();
    }

    public static Builder configure(GeometryProperties properties) {
      return new Builder(properties);
    }

    private Builder() {

    }

    private Builder(GeometryProperties properties) {
      distanceM = properties.distanceM;
      avgSpeedMPS = properties.avgSpeedMPS;
      unsimplifiedPointCount = properties.unsimplifiedPointCount;
      simplifiedPointCount = properties.simplifiedPointCount;
      targetPointCount = properties.targetPointCount;
      otherFields = properties.otherFields;
    }

    public Builder withDistanceM(Double distanceM) {
      this.distanceM = distanceM;
      return this;
    }

    public Builder withAvgSpeedMPS(Double avgSpeedMPS) {
      this.avgSpeedMPS = avgSpeedMPS;
      return this;
    }

    public Builder withUnsimplifiedPointCount(Long unsimplifiedPointCount) {
      this.unsimplifiedPointCount = unsimplifiedPointCount;
      return this;
    }

    public Builder withSimplifiedPointCount(Long simplifiedPointCount) {
      this.simplifiedPointCount = simplifiedPointCount;
      return this;
    }

    public Builder withTargetPointCount(Long targetPointCount) {
      this.targetPointCount = targetPointCount;
      return this;
    }

    @JsonAnySetter
    private Builder withOtherField(String name, Object value) {
      this.otherFields.put(name, value);
      return this;
    }

    public GeometryProperties build() {
      return new GeometryProperties(this);
    }
  }

}

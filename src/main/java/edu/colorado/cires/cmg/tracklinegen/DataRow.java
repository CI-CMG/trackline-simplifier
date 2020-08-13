package edu.colorado.cires.cmg.tracklinegen;

import java.time.Instant;

public interface DataRow {
   Instant getTimestamp();
   Double getLon();
   Double getLat();
}

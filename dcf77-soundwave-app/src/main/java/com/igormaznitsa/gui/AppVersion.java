package com.igormaznitsa.gui;

import java.io.InputStream;
import java.util.Properties;

public final class AppVersion {

  private static final String RESOURCE = "/app.properties";

  private AppVersion() {
  }

  public static String get() {
    final String fromResource = loadFromClasspath();
    if (fromResource != null && !fromResource.isBlank() && !fromResource.startsWith("${")) {
      return fromResource.trim();
    }

    final Package pkg = AppVersion.class.getPackage();
    final String fromManifest = pkg != null ? pkg.getImplementationVersion() : null;
    if (fromManifest != null && !fromManifest.isBlank()) {
      return fromManifest.trim();
    }

    return "development";
  }

  private static String loadFromClasspath() {
    try (InputStream in = AppVersion.class.getResourceAsStream(RESOURCE)) {
      if (in == null) {
        return null;
      }
      final Properties properties = new Properties();
      properties.load(in);
      return properties.getProperty("version");
    } catch (Exception ex) {
      return null;
    }
  }
}

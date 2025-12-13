package com.acantilado.collection.properties.idealista;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ProxyConfiguration {
  @JsonProperty("useApifyProxy")
  private final boolean useApifyProxy;

  @JsonProperty("apifyProxyGroups")
  private final List<String> apifyProxyGroups;

  public ProxyConfiguration(boolean useApifyProxy, List<String> apifyProxyGroups) {
    this.useApifyProxy = useApifyProxy;
    this.apifyProxyGroups = apifyProxyGroups;
  }

  public static ProxyConfiguration datacenter() {
    return new ProxyConfiguration(true, List.of()); // Empty list = datacenter
  }

  public static ProxyConfiguration residential() {
    return new ProxyConfiguration(true, List.of("RESIDENTIAL"));
  }
}

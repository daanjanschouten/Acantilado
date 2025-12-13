package com.acantilado.collection.properties.idealista;

public enum IdealistaOperation {
  SALE("sale"),
  RENT("rent");

  final String operation;

  public String getName() {
    return this.operation;
  }
  ;

  IdealistaOperation(String operation) {
    this.operation = operation;
  }

  public static IdealistaOperation fromOperationCode(String operationCode) {
    for (IdealistaOperation o : values()) {
      if (o.operation.equals(operationCode)) {
        return o;
      }
    }
    throw new IllegalArgumentException();
  }
}

package com.schouten.core.resources.aviation.model;

public class FlightCarriers {
    private String carrierId;
    private String operatorId;

    public FlightCarriers(String carrierId, String operatorId) {
        this.carrierId = carrierId;
        this.operatorId = operatorId;
    }

    public FlightCarriers() {
    }

    public String getCarrierId() {
        return carrierId;
    }

    public String getOperatorId() {
        return operatorId;
    }
}

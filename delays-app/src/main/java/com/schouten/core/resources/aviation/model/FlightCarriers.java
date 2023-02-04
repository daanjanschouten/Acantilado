package com.schouten.core.resources.aviation.model;

import javax.validation.constraints.NotBlank;

public class FlightCarriers {
    @NotBlank
    private String carrierId;
    @NotBlank
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

package com.acantilado.gathering.utils;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.http.HttpRequest;

public interface RequestBodyData {
    HttpRequest.BodyPublisher toRequestBodyString() throws JsonProcessingException;
}
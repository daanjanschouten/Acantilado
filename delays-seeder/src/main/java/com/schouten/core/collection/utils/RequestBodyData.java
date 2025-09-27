package com.schouten.core.collection.utils;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.http.HttpRequest;

public interface RequestBodyData<T> {
    HttpRequest.BodyPublisher toRequestBodyString(T data) throws JsonProcessingException;
}
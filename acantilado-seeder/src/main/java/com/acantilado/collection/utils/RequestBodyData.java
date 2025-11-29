package com.acantilado.collection.utils;

import java.net.http.HttpRequest;

public interface RequestBodyData {
    HttpRequest.BodyPublisher toRequestBodyString();
}
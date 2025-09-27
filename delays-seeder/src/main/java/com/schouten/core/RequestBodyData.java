package com.schouten.core;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.http.HttpRequest;

public interface RequestBodyData<T> {

    HttpRequest.BodyPublisher toRequestBodyString(T data) throws JsonProcessingException;
}
////
////// Your data class
////public class RequestData {
////    public String name;
////    public int age;
////    // constructors, getters, setters
////}
////
////// Sending the request
//ObjectMapper mapper = new ObjectMapper();
//RequestData data = new RequestData("John", 30);
//
//HttpClient client = HttpClient.newHttpClient();
//HttpRequest request = HttpRequest.newBuilder()
//        .uri(URI.create("https://api.example.com/users"))
//        .header("Content-Type", "application/json")
//        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(data)))
//        .build();
//
//HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

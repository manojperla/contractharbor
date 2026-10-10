package com.manojperla.contractharbor;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import static io.restassured.RestAssured.given;

/** A per-client specification: no mutable global RestAssured configuration. */
final class TaskApi {
    private final RequestSpecification spec;
    TaskApi(String baseUrl, String token) {
        var builder = new RequestSpecBuilder().setBaseUri(baseUrl).setContentType(ContentType.JSON);
        if (token != null) builder.addHeader("Authorization", "Bearer " + token);
        spec = builder.build();
    }
    Response create(Object body) { return given().spec(spec).body(body).post("/tasks"); }
    Response get(String id) { return given().spec(spec).get("/tasks/{id}", id); }
    Response delete(String id) { return given().spec(spec).delete("/tasks/{id}", id); }
    Response complete(String id) { return given().spec(spec).body(java.util.Map.of("completed",true)).patch("/tasks/{id}", id); }
}

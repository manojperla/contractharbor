package com.manojperla.contractharbor;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Map;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

class TaskContractTest {
    private DemoApi server;
    private TaskApi api;
    @BeforeEach void start() throws Exception { server = new DemoApi(); api = new TaskApi(server.baseUrl(),server.token); }
    @AfterEach void stop() { if (server != null) server.close(); }
    @Test void createReadCompleteDeleteLifecycle() {
        String id = api.create(Map.of("title","Release checklist")).then().statusCode(201)
            .header("Location",startsWith("/tasks/"))
            .body(matchesJsonSchemaInClasspath("schemas/task.json"))
            .body("title",equalTo("Release checklist"),"completed",equalTo(false)).extract().path("id");
        api.get(id).then().statusCode(200).body("id",equalTo(id));
        api.complete(id).then().statusCode(200).body("completed",equalTo(true));
        api.get(id).then().statusCode(200).body("completed",equalTo(true));
        api.delete(id).then().statusCode(204).body(isEmptyString());
        api.get(id).then().statusCode(404).body("error",equalTo("not_found"));
    }
    @ParameterizedTest @ValueSource(strings={""," ","\t"})
    void rejectsBlankTitles(String title) { api.create(Map.of("title",title)).then().statusCode(400).body("error",equalTo("title_required")); }
    @Test void rejectsMissingTitle() { api.create(Map.of()).then().statusCode(400); }
    @Test void rejectsMalformedJson() { api.create("{broken").then().statusCode(400).body("error",equalTo("invalid_json")); }
    @Test void rejectsMissingAuthentication() { new TaskApi(server.baseUrl(),null).create(Map.of("title","No auth")).then().statusCode(401); }
    @Test void rejectsInvalidAuthentication() { new TaskApi(server.baseUrl(),"invalid").get("unknown").then().statusCode(401); }
    @Test void unknownTaskIsNotFound() { api.get("unknown").then().statusCode(404).body("error",equalTo("not_found")); }
    @Test void preservesUnicodeAndTrimsBoundaryWhitespace() { api.create(Map.of("title","  Mobile ✓  ")).then().statusCode(201).body("title",equalTo("Mobile ✓")); }
}

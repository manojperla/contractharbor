package com.manojperla.contractharbor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory teaching fixture, not a production API. One instance per test. */
final class DemoApi implements AutoCloseable {
    final String token = UUID.randomUUID().toString();
    private final HttpServer server;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, Map<String,Object>> tasks = new ConcurrentHashMap<>();
    DemoApi() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/tasks", this::handle);
        server.start();
    }
    String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    private void handle(HttpExchange x) throws IOException {
        try {
            if (!("Bearer " + token).equals(x.getRequestHeaders().getFirst("Authorization"))) {
                send(x,401,Map.of("error","unauthorized")); return;
            }
            String path = x.getRequestURI().getPath();
            String method = x.getRequestMethod();
            if (path.equals("/tasks") && method.equals("POST")) {
                var node = json.readTree(x.getRequestBody());
                if (node == null || !node.path("title").isTextual() || node.path("title").asText().isBlank()) {
                    send(x,400,Map.of("error","title_required")); return;
                }
                String id = UUID.randomUUID().toString();
                var task = new HashMap<String,Object>(Map.of("id",id,"title",node.get("title").asText().trim(),"completed",false));
                tasks.put(id,task);
                x.getResponseHeaders().add("Location","/tasks/"+id);
                send(x,201,task); return;
            }
            String id = path.startsWith("/tasks/") ? path.substring(7) : "";
            var task = tasks.get(id);
            if (task == null) { send(x,404,Map.of("error","not_found")); return; }
            switch(method) {
                case "GET" -> send(x,200,task);
                case "PATCH" -> {
                    var node = json.readTree(x.getRequestBody());
                    if (node == null || !node.path("completed").isBoolean()) { send(x,400,Map.of("error","completed_required")); return; }
                    task.put("completed",node.get("completed").asBoolean()); send(x,200,task);
                }
                case "DELETE" -> { tasks.remove(id); x.sendResponseHeaders(204,-1); }
                default -> send(x,405,Map.of("error","method_not_allowed"));
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            send(x,400,Map.of("error","invalid_json"));
        } finally { x.close(); }
    }
    private void send(HttpExchange x,int status,Object body) throws IOException {
        byte[] bytes = json.writeValueAsBytes(body);
        x.getResponseHeaders().set("Content-Type","application/json");
        x.sendResponseHeaders(status,bytes.length); x.getResponseBody().write(bytes);
    }
    public void close() { server.stop(0); }
}

package io.quarkus.sample;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import static org.hamcrest.CoreMatchers.is;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TodoResourceTest {

    @Test @Order(1)
    public void testGetAll() {
        given()
            .when()
            .accept(ContentType.JSON)
            .get("/api")
          .then()
             .statusCode(200)
             .body(is(ALL));
    }
    
    @Test @Order(2)
    public void testGet() {
        given()
            .when()
            .accept(ContentType.JSON)
            .get("/api/1")
          .then()
             .statusCode(200)
             .body(is(ONE));
    }
    
    @Test @Order(3)
    public void testCreateNew() {
        given()
            .when()
            .contentType(ContentType.JSON)
            .body(CREATE_NEW)
            .post("/api")
          .then()
             .statusCode(201)
             .body(is(NEW_CREATED));
    }
    
    @Test @Order(4)
    public void testUpdate() {
        given()
            .when()
            .contentType(ContentType.JSON)
            .body(UPDATE)
            .patch("/api/5")
          .then()
             .statusCode(200)
             .body(is(UPDATED));
    }
     
    @Test @Order(5)
    public void testDelete() {
        given()
            .when()
            .delete("/api/5")
          .then()
             .statusCode(204);
    }
    
    private static final String ALL = "[{\"id\":1,\"completed\":true,\"order\":0,\"title\":\"Introduction to Quarkus\"},{\"id\":2,\"completed\":false,\"order\":1,\"title\":\"Hibernate with Panache\"},{\"id\":3,\"completed\":false,\"order\":2,\"title\":\"Visit Quarkus web site\",\"url\":\"https://quarkus.io\"},{\"id\":4,\"completed\":false,\"order\":3,\"title\":\"Star Quarkus project\",\"url\":\"https://github.com/quarkusio/quarkus/\"}]";
    private static final String ONE = "{\"id\":1,\"completed\":true,\"order\":0,\"title\":\"Introduction to Quarkus\"}";
    private static final String CREATE_NEW = "{\"completed\":false,\"order\":0,\"title\":\"Use the REST Endpoint\"}";
    private static final String NEW_CREATED = "{\"id\":5,\"completed\":false,\"order\":0,\"title\":\"Use the REST Endpoint\"}";
    private static final String UPDATE = "{\"id\":5,\"completed\":false,\"order\":0,\"title\":\"Use the GraphQL Endpoint\"}";
    private static final String UPDATED = "{\"id\":5,\"completed\":false,\"order\":0,\"title\":\"Use the GraphQL Endpoint\"}";
}
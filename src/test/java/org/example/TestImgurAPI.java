package org.example;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestImgurAPI
{
    static Map<String, String> headers = new HashMap<>();
    static Properties properties = new Properties();

    @BeforeAll
    static void setUp() throws IOException {
        RestAssured.filters(new AllureRestAssured());

        headers.put("Authorization", "Bearer 37219c516065ca29ba59c7c822eda481c9065c35");

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        FileInputStream file;
        file = new FileInputStream("src/test/resources/imgur.properties");
        properties.load(file);
        file.close();
    }

    @AfterAll
    static void setDown() throws IOException {
        FileOutputStream file;
        file = new FileOutputStream("src/test/resources/imgur.properties");
        properties.store(file, null);
        file.close();
    }

    @Order(1)
    @Test
    void createAlbum() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .param("title", properties.get("albumTitle"))
                .param("description", properties.get("description"))
                .param("privacy", properties.get("privacy"))
                .post(properties.get("createAlbumURL").toString())
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");

        properties.setProperty("albumHash", result.getString("data.id"));
        properties.setProperty("albumDeleteHash", result.getString("data.deletehash"));
    }

    @Order(2)
    @Test
    void updateAlbum() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .param("title", properties.get("editAlbumTitle"))
                .param("description", properties.get("editAlbumDescription"))
                .pathParam("albumHash", properties.get("albumHash"))
                .put("https://api.imgur.com/3/album/{albumHash}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
    }

    @Order(3)
    @Test
    void getAlbumInfo() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("albumHash", properties.get("albumHash"))
                .get("https://api.imgur.com/3/album/{albumHash}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals(properties.get("editAlbumTitle"), result.getString("data.title"), "Title album not updated");
        Assertions.assertEquals(properties.get("editAlbumDescription"), result.getString("data.description"), "Description album not updated");
    }

    @Order(4)
    @Test
    void addAlbumInFavorite() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("albumHash", properties.get("albumHash"))
                .post("https://api.imgur.com/3/album/{albumHash}/favorite")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals("favorited", result.getString("data"), "Not add to favorite");
    }

    @Order(5)
    @Test
    void uploadImage() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .param("image", properties.get("imageUrl"))
                .param("type", "url")
                .param("name", properties.get("imageName"))
                .param("title", properties.get("imageTitle"))
                .post("https://api.imgur.com/3/upload")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        properties.setProperty("imageId", result.getString("data.id"));
        properties.setProperty("imageDeleteHash", result.getString("data.deletehash"));
    }

    @Order(6)
    @Test
    void getImageInfo() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("imageId", properties.get("imageId"))
                .get("https://api.imgur.com/3/image/{imageId}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals(properties.get("imageTitle"), result.getString("data.title"), "Not correct image");
        Assertions.assertTrue(Integer.parseInt(result.getString("data.size")) > 0, "Not correct size");
    }

    @Order(7)
    @Test
    void updateImageInfo() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("imageId", properties.get("imageId"))
                .param("title", properties.get("imageNewTitle"))
                .param("description", properties.get("imageNewDescription"))
                .post("https://api.imgur.com/3/image/{imageId}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
    }

    @Order(8)
    @Test
    void checkTitleImageAfterUpdate() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("imageId", properties.get("imageId"))
                .get("https://api.imgur.com/3/image/{imageId}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals(properties.get("imageNewTitle"), result.getString("data.title"), "Not correct title");
        Assertions.assertEquals(properties.get("imageNewDescription"), result.getString("data.description"), "Not correct description");
    }

    @Order(9)
    @Test
    void getAccountName() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .get("https://api.imgur.com/3/account/me/settings")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertTrue(result.getString("data.account_url").length() > 0, "UserName not correct");

        properties.setProperty("userName", result.getString("data.account_url"));
    }

    @Order(10)
    @Test
    void favoriteImage() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("imageId", properties.get("imageId"))
                .post("https://api.imgur.com/3/image/{imageId}/favorite")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals("favorited", result.getString("data"), "Image favorited error");
    }

    @Order(11)
    @Test
    void checkFavoriteImage() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("userName", properties.get("userName"))
                .pathParam("page", "0")
                .pathParam("favoritesSort", "newest")
                .get("https://api.imgur.com/3/account/{userName}/favorites/{page}/{favoritesSort}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals(properties.get("imageId"), result.getString("data[0].id"), "New Image not in favorite");
    }

    @Order(12)
    @Test
    void unfavoriteImage() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("imageId", properties.get("imageId"))
                .post("https://api.imgur.com/3/image/{imageId}/favorite")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals("unfavorited", result.getString("data"), "Image favorited error");
    }

    @Order(13)
    @Test
    void checkUnfavoriteImage() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("userName", properties.get("userName"))
                .pathParam("page", "0")
                .pathParam("favoritesSort", "newest")
                .get("https://api.imgur.com/3/account/{userName}/favorites/{page}/{favoritesSort}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertNotEquals(properties.get("imageId"), result.getString("data[0].id"), "New Image not in favorite");
    }

    @Order(14)
    @Test
    void addImageToAlbum() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("albumHash", properties.get("albumHash"))
                .param("ids[]", properties.get("imageId"))
                .post("https://api.imgur.com/3/album/{albumHash}/add")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
    }

    @Order(15)
    @Test
    void getAlbumImage() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("albumHash", properties.get("albumHash"))
                .get("https://api.imgur.com/3/album/{albumHash}/images")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals(properties.get("imageId"), result.getString("data[0].id"), "New Image not in album");
    }

    @Order(16)
    @Test
    void uploadImage2() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .param("image", properties.get("imageUrl2"))
                .param("type", "url")
                .param("name", properties.get("imageName2"))
                .param("title", properties.get("imageTitle2"))
                .post("https://api.imgur.com/3/upload")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        properties.setProperty("imageId2", result.getString("data.id"));
        properties.setProperty("imageDeleteHash2", result.getString("data.deletehash"));
    }

    @Order(17)
    @Test
    void addImage2ToAlbum() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("albumHash", properties.get("albumHash"))
                .param("ids[]", properties.get("imageId2"))
                .post("https://api.imgur.com/3/album/{albumHash}/add")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
    }

    @Order(18)
    @Test
    void checkCountImageInAlbum() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("albumHash", properties.get("albumHash"))
                .get("https://api.imgur.com/3/album/{albumHash}/images")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals(2, result.getList("data").size(), "Incorrect image count`");
    }

    @Order(19)
    @Test
    void removeImageFromAlbum() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("albumHash", properties.get("albumHash"))
                .param("ids[]", properties.get("imageId2"))
                .post("https://api.imgur.com/3/album/{albumHash}/remove_images")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
    }

    @Order(20)
    @Test
    void checkCountImageInAlbumAfterRemove() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("albumHash", properties.get("albumHash"))
                .get("https://api.imgur.com/3/album/{albumHash}/images")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
        Assertions.assertEquals(1, result.getList("data").size(), "Incorrect image count`");
    }

    @Order(21)
    @Test
    void deleteFirstImage() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("imageId", properties.get("imageId"))
                .delete("https://api.imgur.com/3/image/{imageId}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
    }

    @Order(22)
    @Test
    void deleteSecondImage() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("imageId", properties.get("imageId2"))
                .delete("https://api.imgur.com/3/image/{imageId}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
    }

    @Order(23)
    @Test
    void deleteAlbum() {
        JsonPath result = given()
                .headers(headers)
                .when()
                .pathParam("albumDeleteHash", properties.get("albumDeleteHash"))
                .delete("https://api.imgur.com/3/album/{albumDeleteHash}")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath();

        Assertions.assertEquals("true", result.getString("success"), "Not success");
    }


    void getAccountInfoTest() {
        given()
                .headers(headers)
                .when()
                .get("https://api.imgur.com/3/account/" + properties.get("username"))
                .then()
                .statusCode(200);
    }

    @Test
    void getAccountNameTest() {
        String usernameImgur = given()
                .headers(headers)
                .when()
                .get("https://api.imgur.com/3/account/me/settings")
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath()
                .getString("data.account_url");

        assertThat(usernameImgur, equalTo(properties.get("username")));
        System.out.println("Username = " + usernameImgur);

    }

}

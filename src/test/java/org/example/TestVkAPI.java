package org.example;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.restassured.RestAssured.given;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestVkAPI {
    static Map<String, String> headers = new HashMap<>();
    static Properties properties = new Properties();

    @BeforeAll
    static void setUp() throws IOException {
        RestAssured.filters(new AllureRestAssured());


        headers.put("Authorization", "Bearer 9cc408c6c86282728bac8f201a39df9cfd4555dca80dbc7b57f6baaa4230765d96f5064de26df1bc799a5");
        // активация логирования, если запрос упал
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        FileInputStream file;
        file = new FileInputStream("src/test/resources/vk.properties");
        properties.load(file);
        file.close();
    }

    @AfterAll
    static void setDown() throws IOException {

        FileOutputStream file;
        file = new FileOutputStream("src/test/resources/vk.properties");
        properties.store(file, null);
        file.close();
    }

    @Order(1)
    @Test
    void getAccountNameTest() {
        String authorOwnerIdVk = given()
                .headers(headers)
                .when()
                .get("https://api.vk.com/method/apps.get?v=" + properties.get("v"))
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath()
                .getString("response.items[0].author_owner_id");

        properties.setProperty("authorOwnerIdVk", authorOwnerIdVk);
    }

    @Order(2)
    @Test
    void photosCreateAlbum() {
        String albumId = given()
                .headers(headers)
                .when()
                .get("https://api.vk.com/method/photos.createAlbum?v=" + properties.get("v") + "&title=" +  properties.get("albumTitle"))
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath()
                .getString("response.id");

        Assertions.assertTrue(albumId.length() > 1, "Album is not created");
        properties.setProperty("albumId", albumId);
    }

    @Order(3)
    @Test
    void getAlbums() {
        given()
                .headers(headers)
                .when()
                .get("https://api.vk.com/method/photos.getAlbums?v=" + properties.get("v"))
                .then()
                .log().all()
                .statusCode(200);
    }

    @Order(4)
    @Test
    void getUploadServerAddress() {
        String uploadUrl = given()
                .headers(headers)
                .when()
                .get("https://api.vk.com/method/photos.getUploadServer?v=" + properties.get("v") + "&album_id=" + properties.get("albumId"))
                .then()
                .statusCode(200)
                .contentType("application/json")
                .extract()
                .response()
                .jsonPath()
                .getString("response.upload_url");

        Assertions.assertTrue(uploadUrl.length() > 10, "Url is not load");
        properties.setProperty("uploadUrl", uploadUrl);
    }

    @Order(5)
    @Test
    void uploadImage() {
        JsonPath res = given()
                .when()
                    .headers(headers)
                    .contentType("multipart/form-data")
                    .multiPart("file1", new File("./src/test/resources/image.jpg"))
                    .post(properties.get("uploadUrl").toString())
                .then()
                    .statusCode(200)
                    .extract()
                    .response()
                    .jsonPath();

        String server = res.getString("server");
        Assertions.assertTrue(server.length() > 0, "Server param is not correct");
        properties.setProperty("server", server);

        String photosList = res.getString("photos_list");
        Assertions.assertTrue(photosList.length() > 2, "PhotosList param is not correct");
        properties.setProperty("photosList", photosList);

        String hash = res.getString("hash");
        Assertions.assertTrue(hash.length() > 0, "Hash param is not correct");
        properties.setProperty("hash", hash);
    }

    @Order(6)
    @Test
    void savePhotos() {
        String vkImageId = given()
                .headers(headers)

                .when()
                .param("photos_list", properties.get("photosList"))
                .get("https://api.vk.com/method/photos.save" +
                        "?v=" + properties.get("v") +
                        "&server=" + properties.get("server") +
                        "&album_id=" + properties.get("albumId") +
                        "&hash=" + properties.get("hash")
                )
                .then()
                    .statusCode(200)
                    .contentType("application/json")
                    .extract()
                    .response()
                    .jsonPath()
                    .getString("response[0].id");

        Assertions.assertTrue(vkImageId.length() > 0, "vkImageId is not correct");
        properties.setProperty("vkImageId", vkImageId);
    }

    @Order(7)
    @Test
    void getPhotosInAlbum() {
        JsonPath res = given()
                .when()
                .headers(headers)
                .log().all()
                .get("https://api.vk.com/method/photos.get?v=" + properties.get("v") + "&owner_id=" + properties.get("authorOwnerIdVk") + "&album_id=" + properties.get("albumId") + "&extended=1")
                .then()
                .statusCode(200)
                .log().all()
                .extract()
                .response()
                .jsonPath();

        String count = res.getString("response.count");
        Assertions.assertTrue(count.length() > 0, "Photo was not saved");

        String currentVKImageId = res.getString("response.items[0].id");
        System.out.println(currentVKImageId);
        System.out.println(properties.get("vkImageId"));
        Assertions.assertTrue(currentVKImageId.equals(properties.get("vkImageId")), "vkImageId is not correct");
    }

    @Order(8)
    @Test
    void editAlbum() {
        given()
                .headers(headers)
                .log().all()
                .when()
                .param("title", properties.get("newTitle"))
                .param("description", properties.get("description"))
                .post("https://api.vk.com/method/photos.editAlbum?v=" + properties.get("v") + "&owner_id=" + properties.get("authorOwnerIdVk") + "&album_id=" + properties.get("albumId"))
                .prettyPeek()
                .then()
                .statusCode(200);
    }

    @Order(9)
    @Test
    void getAlbumsInfoAfterEdit() {
        JsonPath res = given()
                .headers(headers)
                .when()
                .get("https://api.vk.com/method/photos.getAlbums?v=" + properties.get("v"))
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath();

        String newTitle = res.getString("response.items[0].title");
        Assertions.assertEquals(newTitle, properties.get("newTitle"));

        String description = res.getString("response.items[0].description");
        Assertions.assertEquals(description, properties.get("description"));
    }
}

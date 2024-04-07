package uz.jarvis.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.util.EntityUtils;

public class ApiService {
  public static String createTree(
    String fullName,
    String type,
    String latitude,
    String longitude,
    String photoId
  ) throws IOException {
    String apiUrl = "http://192.168.1.150:8080/api/v1/tree/create";

    // JSON request body
    String jsonBody = "{\n" +
      "  \"fullName\": \"" + fullName + "\",\n" +
      "  \"type\": \"" + type + "\",\n" +
      "  \"latitude\": " + latitude + ",\n" +
      "  \"longitude\": " + longitude + ",\n" +
      "  \"photoId\": \"" + photoId + "\"\n" +
      "}";

    // Create HTTP client
    HttpClient httpClient = HttpClients.createDefault();

    // Create HTTP POST request
    HttpPost httpPost = new HttpPost(apiUrl);
    httpPost.addHeader("Content-Type", "application/json");
    httpPost.addHeader("accept", "*/*");

    // Set JSON request body
    httpPost.setEntity(new StringEntity(jsonBody));

    // Execute the request and get the response
    HttpResponse response = httpClient.execute(httpPost);

    // Check if the response is successful
    if (response.getStatusLine().getStatusCode() == 200) {
      // Get response entity
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        // Convert the response content to byte array
        byte[] content = entity.getContent().readAllBytes();

        // Save the byte array as a .jpg file
        Path filePath = Paths.get("tree_image.jpg");
        Files.write(filePath, content);

        // Return the file path as a string
        return filePath.toString();
      }
    }

    // Return null if request failed or response is not successful
    return null;
  }

  public static String uploadImage(String filePath) throws IOException {
    // Create HTTP client
    HttpClient httpClient = HttpClients.createDefault();

    // API endpoint
    String apiUrl = "http://192.168.1.150:8080/attach/upload"; // Update with your API URL

    // Create HTTP POST request
    HttpPost httpPost = new HttpPost(apiUrl);

    // Create multipart entity
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

    // Add image file
    File file = new File(filePath);
    FileBody fileBody = new FileBody(file, ContentType.IMAGE_JPEG, file.getName());
    builder.addPart("file", fileBody);

    // Set multipart entity
    HttpEntity multipart = builder.build();
    httpPost.setEntity(multipart);

    // Execute the request
    HttpResponse response = httpClient.execute(httpPost);

    // Extract response ID if available
    String responseId = null;
    HttpEntity responseEntity = response.getEntity();
    if (responseEntity != null) {
      String responseContent = EntityUtils.toString(responseEntity);
      responseId = extractResponseIdFromJson(responseContent);
    }

    return responseId;
  }

  private static String extractResponseIdFromJson(String responseContent) {
    System.out.println(responseContent);
    // Parse JSON response and extract ID
    JsonParser parser = new JsonParser();
    JsonObject jsonResponse = parser.parse(responseContent).getAsJsonObject();

    // Assuming the ID is stored under the key "id"
    String id = jsonResponse.get("id").getAsString();

    return id;
  }

}

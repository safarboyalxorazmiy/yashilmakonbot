package uz.jarvis.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;


public class ApiService {
  public static String createTree(
    String fullName,
    String type,
    String latitude,
    String longitude,
    String photoPath
  ) throws IOException {
    String apiUrl = "http://192.168.0.106:8080/api/v1/tree/create";

    // JSON request body
    String jsonBody = "{\n" +
      "  \"fullName\": \"string\",\n" +
      "  \"type\": \"string\",\n" +
      "  \"latitude\": 0,\n" +
      "  \"longitude\": 0,\n" +
      "  \"photoPath\": \"string\"\n" +
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
}

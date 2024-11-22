package com.example.MySQL.PostalCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FacebookAuthService {
    private static final String GRAPH_API_URL = "https://graph.facebook.com/v17.0/me/permissions";
    @Value("${facebook.app.id}")
    private String appId;

    @Value("${facebook.app.secret}")
    private String appSecret;

    @Value("${facebook.oauth.url}")
    private String oauthUrl;

    @Value("${whatsapp.api.url}")
    private String apiUrl;

    @Value("${whatsapp.phone.number.id}")
    private String phoneNumberId;




    @GetMapping("accessToken")
    public String AccessTokenGenerate()
    {
        RestTemplate restTemplate = new RestTemplate();

        // Build the URL with parameters
        String requestUrl = String.format("%s?grant_type=client_credentials&client_id=%s&client_secret=%s",
                oauthUrl, appId, appSecret);

        // Send GET request to Facebook OAuth API
        ResponseEntity<Map> response = restTemplate.getForEntity(requestUrl, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // Extract the access token from the response
            Map<String, Object> responseBody = response.getBody();
            return responseBody.get("access_token").toString();
        } else {
            throw new RuntimeException("Failed to generate access token: " + response.getStatusCode());
        }
    }

    @PostMapping("/send-message")
    public String sendMessage(@RequestParam String to, @RequestParam String message) {
        return this.sendMessageM(to, message);
    }



    public String sendMessageM(String toNumber, String msg) {
        String accessToken =  this.AccessTokenGenerate();

        // Construct the endpoint to send messages
        String endpoint = String.format("%s/%s/messages", apiUrl, phoneNumberId);

        // Create the message payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", toNumber);
        payload.put("type", "text");

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("body", msg);
        payload.put("text", textContent);

        // Send the message request
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return "Message sent successfully!";
        } else {
            throw new RuntimeException("Failed to send message: " + response.getBody());
        }
    }
    @GetMapping("permissions")
    public ResponseEntity<String> getFacebookPermissions() {
        System.out.println("AccessToken:--"+AccessTokenGenerate());
        RestTemplate restTemplate = new RestTemplate();

        // Build the URL with the access token as a query parameter
        String url = UriComponentsBuilder.fromHttpUrl(GRAPH_API_URL)
                .queryParam("access_token", AccessTokenGenerate())
                .toUriString();

        try {
            // Make the GET request
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // Return the response body
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            // Return error message if something goes wrong
            return ResponseEntity.status(500).body("Error fetching permissions: " + e.getMessage());
        }
    }
    public String getPhoneNumberDetails(String phoneNumberId) {
        RestTemplate restTemplate = new RestTemplate();
        String accessToken = this.AccessTokenGenerate();

        // Construct the API URL for fetching phone number details
        String endpoint = String.format("%s/%s", apiUrl, phoneNumberId);

        // Set the headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        // Make the GET request
        ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.GET, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to fetch phone number details: " + response.getBody());
        }
    }
    @GetMapping("/facebook/phone-numbers")
    public ResponseEntity<String> getPhoneNumber() {
        RestTemplate restTemplate = new RestTemplate();

        // Replace with your Facebook Business Account ID
        String businessAccountId = "<your_business_account_id>";

        // Build the URL with access token as a query parameter
        String url = UriComponentsBuilder.fromHttpUrl(GRAPH_API_URL)
                .queryParam("access_token", AccessTokenGenerate())
                .buildAndExpand(businessAccountId)
                .toUriString();

        try {
            // Make the GET request
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // Return the response body
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            // Handle any exceptions
            return ResponseEntity.status(500).body("Error fetching phone numbers: " + e.getMessage());
        }
    }
    @GetMapping("business-id")
    public ResponseEntity<String> getBusinessId() {
        RestTemplate restTemplate = new RestTemplate();

        // Build the URL to get user or app business account info
        String url = UriComponentsBuilder.fromHttpUrl(GRAPH_API_URL)
                .queryParam("fields", "id,name,accounts")
                .queryParam("access_token", AccessTokenGenerate())
                .toUriString();

        try {
            // Make the GET request
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // Return the response body
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            // Handle any exceptions
            return ResponseEntity.status(500).body("Error fetching business ID: " + e.getMessage());
        }
    }
}

package com.example.demo.endpoint.rest.controller.health;

import com.example.demo.PojaGenerated;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@PojaGenerated
@RestController
@RequiredArgsConstructor
public class HazavaoController {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @GetMapping(value = "/hazavao", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> hazavao(@RequestParam("teny") String teny) {
        if (teny == null || teny.isBlank()) {
            return ResponseEntity.badRequest().body("Paramètre 'teny' manquant");
        }

        try {
            String prompt = "Donne-moi la définition en malgache du mot : \"" + teny + "\".";
            String requestBody = """
                {
                  "model": "gpt-3.5-turbo",
                  "messages": [
                    {"role": "user", "content": "%s"}
                  ],
                  "max_tokens": 100,
                  "temperature": 0.5
                }
                """.formatted(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Erreur OpenAI : " + response.body());
            }

            String definition = extractDefinition(response.body());

            if (definition == null || definition.isBlank()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Définition introuvable");
            }

            return ResponseEntity.ok(definition);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur serveur : " + e.getMessage());
        }
    }

    private String extractDefinition(String responseBody) {
        int idx = responseBody.indexOf("\"content\":\"");
        if (idx < 0) return null;
        int start = idx + 10;
        int end = responseBody.indexOf("\"", start);
        if (end < 0) return null;
        String content = responseBody.substring(start, end);
        return content.replace("\\n", "\n").replace("\\\"", "\"");
    }
}

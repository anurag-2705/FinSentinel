package com.finsentinel.ml.service;

import com.finsentinel.ml.dto.MLPredictionRequest;
import com.finsentinel.ml.dto.MLPredictionResponse;
import com.finsentinel.ml.dto.MLExplanationResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class MLPredictionService {

    private final String mlServiceUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MLPredictionService(
            @Value("${finsentinel.ml.service.url}") String mlServiceUrl,
            ObjectMapper objectMapper
    ) {
        this.mlServiceUrl = mlServiceUrl;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public MLPredictionResponse predict(MLPredictionRequest request) {

        try {
            String jsonBody = objectMapper.writeValueAsString(request);

            byte[] requestBytes = jsonBody.getBytes(StandardCharsets.UTF_8);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(mlServiceUrl + "/predict"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBytes))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                throw new RuntimeException(
                        "ML Service Error: " + response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    MLPredictionResponse.class
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not communicate with ML service", e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "ML request was interrupted", e
            );
        }
    }

    public MLExplanationResponse explain(MLPredictionRequest request) {

        try {
            String jsonBody = objectMapper.writeValueAsString(request);

            byte[] requestBytes = jsonBody.getBytes(StandardCharsets.UTF_8);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(mlServiceUrl + "/explain"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBytes))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                throw new RuntimeException(
                        "SHAP Explanation Service Error: " + response.body()
                );
            }

            return objectMapper.readValue(
                    response.body(),
                    MLExplanationResponse.class
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Could not communicate with SHAP explanation service", e
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "SHAP explanation request was interrupted", e
            );
        }
    }
}
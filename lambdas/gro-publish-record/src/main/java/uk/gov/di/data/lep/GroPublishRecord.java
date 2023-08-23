package uk.gov.di.data.lep;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.data.lep.dto.CognitoTokenResponse;
import uk.gov.di.data.lep.exceptions.AuthException;
import uk.gov.di.data.lep.exceptions.GroApiCallException;
import uk.gov.di.data.lep.library.config.Config;
import uk.gov.di.data.lep.library.dto.GroJsonRecord;
import uk.gov.di.data.lep.library.exceptions.MappingException;
import uk.gov.di.data.lep.library.services.AwsService;
import uk.gov.di.data.lep.library.services.Mapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GroPublishRecord implements RequestStreamHandler {
    private final AwsService awsService;
    private final Config config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    protected static Logger logger = LogManager.getLogger();

    public GroPublishRecord() {
        this(new AwsService(), new Config(), HttpClient.newHttpClient(), Mapper.objectMapper());
    }

    public GroPublishRecord(AwsService awsService, Config config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.awsService = awsService;
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Tracing
    @Logging(clearState = true)
    public void handleRequest(InputStream input, OutputStream output, Context context) {
        var event = readInputStream(input);
        logger.info("Received record: {}", event.registrationID());
        var authorisationToken = getAuthorisationToken();
        postRecordToLifeEvents(event, authorisationToken);
    }

    @Tracing
    private GroJsonRecord readInputStream(InputStream input) {
        try {
            return objectMapper.readValue(input, GroJsonRecord.class);
        } catch (IOException e) {
            logger.error("Failed to map Input Stream to GRO JSON record");
            throw new MappingException(e);
        }
    }

    // In this case, the fact that the thread has been interrupted is captured in our message and exception stack,
    // and we do not need to rethrow the same exception
    @SuppressWarnings("java:S2142")
    @Tracing
    private String getAuthorisationToken() {
        var clientId = config.getCognitoClientId();
        var clientSecret = awsService.getCognitoClientSecret(config.getUserPoolId(), clientId);
        var authorisationRequest = HttpRequest.newBuilder()
            .uri(URI.create(config.getCognitoOauth2TokenUri()))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(String.format(
                "grant_type=client_credentials&client_id=%s&client_secret=%s",
                clientId,
                clientSecret
            )))
            .build();

        try {
            logger.info("Sending authorisation request");
            var response = httpClient.send(authorisationRequest, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), CognitoTokenResponse.class).accessToken();
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to send authorisation request");
            throw new AuthException("Failed to send authorisation request", e);
        }
    }

    // In this case, the fact that the thread has been interrupted is captured in our message and exception stack,
    // and we do not need to rethrow the same exception
    @SuppressWarnings("java:S2142")
    @Tracing
    private void postRecordToLifeEvents(GroJsonRecord event, String authorisationToken) {

        var requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(String.format("https://%s/events/deathNotification", config.getLifeEventsPlatformDomain())))
            .header("Authorization", authorisationToken);

        try {
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            logger.error("Failed to map GRO JSON record to string");
            throw new MappingException(e);
        }

        try {
            logger.info("Sending GRO record request: {}", event.registrationID());
            httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to send GRO record request");
            throw new GroApiCallException("Failed to send GRO record request", e);
        }
    }
}

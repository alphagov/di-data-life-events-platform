package uk.gov.di.data.lep.library;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import uk.gov.di.data.lep.library.config.Config;
import uk.gov.di.data.lep.library.dto.GroDeathEventEnrichedData;
import uk.gov.di.data.lep.library.enums.GroSex;
import uk.gov.di.data.lep.library.services.AwsService;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LambdaHandlerTest {
    private static final Config config = mock(Config.class);
    private static final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private static final LambdaLogger logger = mock(LambdaLogger.class);
    private static MockedStatic<AwsService> awsService;
    private static final LambdaHandler<GroDeathEventEnrichedData> underTest = new TestLambda(config, objectMapper);

    @BeforeAll
    public static void setup() {
        awsService = mockStatic(AwsService.class);
        underTest.logger = logger;
    }

    @BeforeEach
    public void refreshSetup() {
        clearInvocations(config);
        clearInvocations(logger);
        clearInvocations(objectMapper);
    }

    @AfterAll
    public static void tearDown() {
        awsService.close();
    }

    @Test
    public void publishPublishesMessageToQueue() throws JsonProcessingException {
        var output = new GroDeathEventEnrichedData(
            "123a1234-a12b-12a1-a123-123456789012",
            GroSex.FEMALE,
            LocalDate.parse("1972-02-20"),
            LocalDate.parse("2021-12-31"),
            "123456789",
            LocalDateTime.parse("2022-01-05T12:03:52.123"),
            "1",
            "12",
            "2021",
            "Bob Burt",
            "Smith",
            "Jane",
            "888 Death House",
            "8 Death lane",
            "Deadington",
            "Deadshire",
            "XX1 1XX"
        );

        when(config.getTargetQueue()).thenReturn("targetQueueURL");
        when(objectMapper.writeValueAsString(output)).thenReturn("mappedQueueOutput");

        underTest.publish(output);

        verify(logger).log("Putting message on target queue: targetQueueURL");

        awsService.verify(() -> AwsService.putOnQueue("mappedQueueOutput"));
    }

    @Test
    public void publishPublishesMessageToTopic() throws JsonProcessingException {
        var output = new GroDeathEventEnrichedData(
            "123a1234-a12b-12a1-a123-123456789012",
            GroSex.FEMALE,
            LocalDate.parse("1972-02-20"),
            LocalDate.parse("2021-12-31"),
            "123456789",
            LocalDateTime.parse("2022-01-05T12:03:52"),
            "1",
            "12",
            "2021",
            "Bob Burt",
            "Smith",
            "Jane",
            "888 Death House",
            "8 Death lane",
            "Deadington",
            "Deadshire",
            "XX1 1XX"
        );
        when(config.getTargetTopic()).thenReturn("targetTopicARN");
        when(objectMapper.writeValueAsString(output)).thenReturn("mappedTopicOutput");

        underTest.publish(output);

        verify(logger).log("Putting message on target topic: targetTopicARN");

        awsService.verify(() -> AwsService.putOnTopic("mappedTopicOutput"));
    }

    static class TestLambda extends LambdaHandler<GroDeathEventEnrichedData> {
        public TestLambda(Config config, ObjectMapper objectMapper) {
            super(config, objectMapper);
        }
    }
}

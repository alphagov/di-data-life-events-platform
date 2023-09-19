package uk.gov.di.data.lep;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.lambda.powertools.logging.Logging;
import software.amazon.lambda.powertools.tracing.Tracing;
import uk.gov.di.data.lep.library.LambdaHandler;
import uk.gov.di.data.lep.library.config.Config;
import uk.gov.di.data.lep.library.dto.GroJsonRecordWithCorrelationId;
import uk.gov.di.data.lep.library.dto.deathnotification.audit.DeathEnrichmentAudit;
import uk.gov.di.data.lep.library.dto.deathnotification.audit.DeathEnrichmentAuditExtensions;
import uk.gov.di.data.lep.library.dto.deathnotification.DeathNotificationSet;
import uk.gov.di.data.lep.library.dto.deathnotification.DeathNotificationSetMapper;
import uk.gov.di.data.lep.library.exceptions.MappingException;
import uk.gov.di.data.lep.library.services.AwsService;

public class DeathEnrichment
    extends LambdaHandler<DeathNotificationSet>
    implements RequestHandler<SQSEvent, String> {

    public DeathEnrichment() {
    }

    public DeathEnrichment(AwsService awsService, Config config, ObjectMapper objectMapper) {
        super(awsService, config, objectMapper);
    }

    @Override
    @Tracing
    @Logging(clearState = true)
    public String handleRequest(SQSEvent sqsEvent, Context context) {
        try {
            var sqsMessage = sqsEvent.getRecords().get(0);
            var baseData = objectMapper.readValue(sqsMessage.getBody(), GroJsonRecordWithCorrelationId.class);
            var enrichedData = enrichData(baseData);

            var auditData = generateAuditData(enrichedData);
            addAuditDataToQueue(auditData);

            return mapAndPublish(enrichedData);
        } catch (JsonProcessingException e) {
            logger.error("Failed to enrich request due to mapping error");
            throw new MappingException(e);
        }
    }

    @Tracing
    private DeathNotificationSet enrichData(GroJsonRecordWithCorrelationId baseData) {
        logger.info("Enriching and mapping data (sourceId: {})", baseData.groJsonRecord().registrationID());

        return DeathNotificationSetMapper.generateDeathNotificationSet(baseData);
    }

    @Tracing
    private DeathEnrichmentAudit generateAuditData(DeathNotificationSet enrichedData) {
        var auditDataExtensions = new DeathEnrichmentAuditExtensions(enrichedData.hashCode(), enrichedData.txn());
        return new DeathEnrichmentAudit(auditDataExtensions);
    }
}

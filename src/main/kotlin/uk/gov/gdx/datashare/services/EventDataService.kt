package uk.gov.gdx.datashare.services

import com.amazonaws.xray.spring.aop.XRayEnabled
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.gdx.datashare.config.AuthenticationFacade
import uk.gov.gdx.datashare.config.ConsumerSubscriptionNotFoundException
import uk.gov.gdx.datashare.config.DateTimeHandler
import uk.gov.gdx.datashare.config.EventNotFoundException
import uk.gov.gdx.datashare.enums.EventType
import uk.gov.gdx.datashare.models.EventNotification
import uk.gov.gdx.datashare.models.Events
import uk.gov.gdx.datashare.repositories.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
@XRayEnabled
class EventDataService(
  private val authenticationFacade: AuthenticationFacade,
  private val consumerSubscriptionRepository: ConsumerSubscriptionRepository,
  private val eventDataRepository: EventDataRepository,
  private val deathRegistrationLookupService: DeathRegistrationLookupService,
  private val prisonerLookupService: PrisonerLookupService,
  private val dateTimeHandler: DateTimeHandler,
  private val meterRegistry: MeterRegistry,
  private val consumerSubscriptionEnrichmentFieldRepository: ConsumerSubscriptionEnrichmentFieldRepository,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getEvent(
    id: UUID,
  ): EventNotification {
    val clientId = authenticationFacade.getUsername()
    val event = eventDataRepository.findByClientIdAndId(clientId, id)
      ?: throw EventNotFoundException("Event $id not found for polling client $clientId")
    val consumerSubscription = consumerSubscriptionRepository.findByEventId(id)
      ?: throw ConsumerSubscriptionNotFoundException("Consumer subscription not found for event $id")

    return mapEventNotification(event, consumerSubscription, includeData = true, callbackEvent = true)
  }

  fun getEvents(
    eventTypes: List<EventType>?,
    optionalStartTime: LocalDateTime?,
    optionalEndTime: LocalDateTime?,
    pageNumber: Int,
    pageSize: Int,
  ): Events {
    val startTime = optionalStartTime ?: dateTimeHandler.defaultStartTime()
    val endTime = optionalEndTime ?: dateTimeHandler.now()
    val clientId = authenticationFacade.getUsername()

    val consumerSubscriptions = eventTypes?.let {
      consumerSubscriptionRepository.findAllByEventTypesAndClientId(clientId, eventTypes)
    } ?: consumerSubscriptionRepository.findAllByClientId(clientId)

    if (consumerSubscriptions.isEmpty()) {
      return Events(0, emptyList())
    }

    val consumerSubscriptionIdMap = consumerSubscriptions.toList().associateBy({ it.id }, { it })

    val events = eventDataRepository.findPageByConsumerSubscriptions(
      consumerSubscriptionIdMap.keys.toList(),
      startTime,
      endTime,
      pageSize,
      (pageSize * pageNumber),
    )

    val eventModels = events.map { event ->
      val subscription = consumerSubscriptionIdMap[event.consumerSubscriptionId]!!
      mapEventNotification(event, subscription, subscription.enrichmentFieldsIncludedInPoll)
    }

    val eventsCount = eventDataRepository.countByConsumerSubscriptions(
      consumerSubscriptionIdMap.keys.toList(),
      startTime,
      endTime,
    )

    return Events(eventsCount, eventModels)
  }

  fun deleteEvent(id: UUID): EventNotification {
    val callbackClientId = authenticationFacade.getUsername()
    val event = eventDataRepository.findByClientIdAndId(callbackClientId, id)
      ?: throw EventNotFoundException("Event $id not found for callback client $callbackClientId")
    val consumerSubscription = consumerSubscriptionRepository.findByEventId(id)
      ?: throw ConsumerSubscriptionNotFoundException("Consumer subscription not found for event $id")

    eventDataRepository.softDeleteById(event.id, dateTimeHandler.now())
    meterRegistry.counter(
      "EVENT_ACTION.EventDeleted",
      "eventType",
      consumerSubscription.eventType.name,
      "consumerSubscription",
      event.consumerSubscriptionId.toString(),
    ).increment()
    meterRegistry.timer("DATA_PROCESSING.TimeFromCreationToDeletion")
      .record(Duration.between(event.whenCreated, dateTimeHandler.now()).abs())
    return mapEventNotification(event, consumerSubscription, false)
  }

  private fun mapEventNotification(
    event: EventData,
    subscription: ConsumerSubscription,
    includeData: Boolean = false,
    callbackEvent: Boolean = false,
  ): EventNotification {
    val enrichmentFields =
      consumerSubscriptionEnrichmentFieldRepository.findAllByConsumerSubscriptionId(subscription.consumerSubscriptionId)
        .map { it.enrichmentField }
    return EventNotification(
      eventId = event.id,
      eventType = subscription.eventType,
      sourceId = event.dataId,
      dataIncluded = if (!callbackEvent) includeData else null,
      enrichmentFields = if (!callbackEvent) enrichmentFields else null,
      eventData = if (includeData) callbackAndEnrichData(subscription, event, enrichmentFields) else null,
    )
  }

  @Suppress("IMPLICIT_CAST_TO_ANY")
  private fun callbackAndEnrichData(
    subscription: ConsumerSubscription,
    event: EventData,
    enrichmentFields: List<String>,
  ) = when (subscription.eventType) {
    EventType.DEATH_NOTIFICATION -> deathRegistrationLookupService.getEnrichedPayload(event.dataId, enrichmentFields)
    EventType.ENTERED_PRISON -> prisonerLookupService.getEnrichedPayload(event.dataId, enrichmentFields)
    else -> log.warn("Not handling this event type {}", subscription.eventType)
  }
}

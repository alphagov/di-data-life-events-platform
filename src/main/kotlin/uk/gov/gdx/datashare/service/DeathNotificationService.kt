package uk.gov.gdx.datashare.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import uk.gov.gdx.datashare.repository.EgressEventData
import uk.gov.gdx.datashare.repository.EgressEventDataRepository
import uk.gov.gdx.datashare.repository.EgressEventTypeRepository
import uk.gov.gdx.datashare.repository.IngressEventData
import java.time.LocalDate
import java.util.*

@Service
class DeathNotificationService(
  private val egressEventTypeRepository: EgressEventTypeRepository,
  private val egressEventDataRepository: EgressEventDataRepository,
  private val eventPublishingService: EventPublishingService,
  private val levApiService: LevApiService,
  private val mapper: ObjectMapper
) {
  suspend fun saveDeathNotificationEvents(
    eventData: IngressEventData,
    details: DataProcessor.DataDetail,
    dataProcessorMessage: DataProcessorMessage
  ) {
    val egressTypes = egressEventTypeRepository.findAllByIngressEventType(eventData.eventTypeId)

    val egressEventData = egressTypes.map {
      val dataPayload =
        enrichData(
          it.enrichmentFields.split(",").toList(),
          dataProcessorMessage.datasetId,
          details.id,
          details.data as String?
        )

      val egressEventId = UUID.randomUUID()
      EgressEventData(
        eventId = egressEventId,
        typeId = it.id,
        ingressEventId = eventData.eventId,
        datasetId = dataProcessorMessage.datasetId,
        dataId = details.id,
        dataPayload = dataPayload?.let { mapper.writeValueAsString(dataPayload) },
        whenCreated = dataProcessorMessage.eventTime,
        dataExpiryTime = dataProcessorMessage.eventTime.plusHours(1)
      )
    }.toList()

    val savedEgressEvents = egressEventDataRepository.saveAll(egressEventData).toList()

    savedEgressEvents.forEach {
      eventPublishingService.storeAndPublishEvent(it.eventId, dataProcessorMessage)
    }
  }

  fun mapDeathNotification(dataPayload: String): DeathNotificationDetails? =
    mapper.readValue(dataPayload, DeathNotificationDetails::class.java)

  private suspend fun enrichData(
    enrichmentFields: List<String>,
    dataset: String,
    dataId: String,
    dataPayload: String?
  ): DeathNotificationDetails? = EnrichmentService.enrichFields(
    getAllEnrichedData(dataset, dataId, dataPayload),
    DeathNotificationDetails(),
    enrichmentFields
  )

  private suspend fun getAllEnrichedData(
    dataset: String,
    dataId: String,
    dataPayload: String?
  ): DeathNotificationDetails? = when (dataset) {
    "DEATH_LEV" -> {
      // get the data from the LEV
      val citizenDeathId = dataId.toInt()
      levApiService.findDeathById(citizenDeathId)
        .map {
          DeathNotificationDetails(
            firstName = it.deceased.forenames,
            lastName = it.deceased.surname,
            dateOfBirth = it.deceased.dateOfBirth,
            dateOfDeath = it.deceased.dateOfDeath,
            sex = it.deceased.sex,
            address = it.deceased.address
          )
        }.first()
    }

    "DEATH_CSV" -> {
      // get the data from the data store - it's a CSV file
      val csvLine = dataPayload!!.split(",").toTypedArray()
      DeathNotificationDetails(
        firstName = csvLine[1],
        lastName = csvLine[0],
        dateOfBirth = LocalDate.parse(csvLine[2]),
        dateOfDeath = LocalDate.parse(csvLine[3]),
        sex = csvLine[4],
        address = if (csvLine.count() > 5) { csvLine[5] } else { null }
      )
    }

    "PASS_THROUGH" -> {
      null
    }

    else -> {
      throw RuntimeException("Unknown DataSet $dataset")
    }
  }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Death notification")
data class DeathNotificationDetails(
  @Schema(description = "First name", required = false, example = "Bob")
  val firstName: String? = null,
  @Schema(description = "Last name", required = false, example = "Smith")
  val lastName: String? = null,
  @Schema(description = "Date of Birth", required = false, example = "2001-12-31T12:34:56")
  val dateOfBirth: LocalDate? = null,
  @Schema(description = "Date of Death", required = false, example = "2021-12-31T12:34:56")
  val dateOfDeath: LocalDate? = null,
  @Schema(description = "Address", required = false, example = "888 Death House, 8 Death lane, Deadington, Deadshire")
  val address: String? = null,
  @Schema(description = "Sex", required = false, example = "Male")
  val sex: String? = null,
)

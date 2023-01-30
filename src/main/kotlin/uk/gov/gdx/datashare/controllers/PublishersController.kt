package uk.gov.gdx.datashare.controllers

import com.amazonaws.xray.spring.aop.XRayEnabled
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import uk.gov.gdx.datashare.models.PublisherRequest
import uk.gov.gdx.datashare.models.PublisherSubRequest
import uk.gov.gdx.datashare.services.PublishersService
import java.util.*

@RestController
@RequestMapping("/publishers", produces = [MediaType.APPLICATION_JSON_VALUE])
@PreAuthorize("hasAnyAuthority('SCOPE_events/admin')")
@Validated
@XRayEnabled
@Tag(name = "12. Publishers")
class PublishersController(
  private val publishersService: PublishersService,
) {
  @GetMapping
  @Operation(
    summary = "Get Publishers",
    description = "Need scope of events/admin",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Publishers",
      ),
    ],
  )
  fun getPublishers() = publishersService.getPublishers()

  @PostMapping
  @Operation(
    summary = "Add Publisher",
    description = "Need scope of events/admin",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Publisher Added",
      ),
    ],
  )
  fun addPublisher(
    @Schema(
      description = "Publisher",
      required = true,
      implementation = PublisherRequest::class,
    )
    @RequestBody
    publisherRequest: PublisherRequest,
  ) = publishersService.addPublisher(publisherRequest)

  @GetMapping("/subscriptions")
  @Operation(
    summary = "Get Publisher Subscriptions",
    description = "Need scope of events/admin",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Publisher Subscriptions",
      ),
    ],
  )
  fun getPublisherSubscriptions() = publishersService.getPublisherSubscriptions()

  @GetMapping("/{publisherId}/subscriptions")
  @Operation(
    summary = "Get Publisher Subscriptions for Publisher",
    description = "Need scope of events/admin",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Publisher Subscriptions",
      ),
    ],
  )
  fun getSubscriptionsForPublisher(
    @Schema(description = "Publisher ID", required = true, example = "00000000-0000-0001-0000-000000000000")
    @PathVariable
    publisherId: UUID,
  ) = publishersService.getSubscriptionsForPublisher(publisherId)

  @PostMapping("/{publisherId}/subscriptions")
  @Operation(
    summary = "Add Publisher Subscription",
    description = "Need scope of events/admin",
    responses = [
      ApiResponse(
        responseCode = "201",
        description = "Publisher Subscription Added",
      ),
    ],
  )
  fun addPublisherSubscription(
    @Schema(description = "Publisher ID", required = true, example = "00000000-0000-0001-0000-000000000000")
    @PathVariable
    publisherId: UUID,
    @Schema(
      description = "Publisher Subscription",
      required = true,
      implementation = PublisherSubRequest::class,
    )
    @RequestBody
    publisherSubRequest: PublisherSubRequest,
  ) = publishersService.addPublisherSubscription(publisherId, publisherSubRequest)

  @PutMapping("/{publisherId}/subscriptions/{subscriptionId}")
  @Operation(
    summary = "Update Publisher",
    description = "Need scope of events/admin",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Publisher Subscription Updated",
      ),
    ],
  )
  fun updatePublisherSubscription(
    @Schema(description = "Publisher ID", required = true, example = "00000000-0000-0001-0000-000000000000")
    @PathVariable
    publisherId: UUID,
    @Schema(
      description = "Publisher Subscription ID",
      required = true,
      example = "00000000-0000-0001-0000-000000000000",
    )
    @PathVariable
    subscriptionId: UUID,
    @Schema(
      description = "Publisher Subscription to update",
      required = true,
      implementation = PublisherSubRequest::class,
    )
    @RequestBody
    publisherSubRequest: PublisherSubRequest,
  ) = publishersService.updatePublisherSubscription(publisherId, subscriptionId, publisherSubRequest)
}

package uk.gov.gdx.datashare.models

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import uk.gov.gdx.datashare.enums.Sex
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Prisoner Details")
data class PrisonerDetails(

  @Schema(description = "Prisoner Number", required = true, example = "A1234BB")
  val prisonerNumber: String? = null,

  @Schema(description = "Forenames of the prisoner", required = true, example = "Bob")
  val firstName: String? = null,

  @Schema(description = "Middle names of the prisoner", required = true, example = "Bert Paul")
  val middleNames: String? = null,

  @Schema(description = "Surname of the prisoner", required = true, example = "Smith")
  val lastName: String? = null,

  @Schema(description = "Sex of the prisoner", required = true, example = "Female")
  val sex: Sex? = null,

  @Schema(description = "Date the prisoner was born", required = false, example = "2001-12-31", type = "date")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  val dateOfBirth: LocalDate? = null,
)

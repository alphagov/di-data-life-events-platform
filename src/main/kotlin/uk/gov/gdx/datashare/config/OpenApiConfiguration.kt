package uk.gov.gdx.datashare.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(
  buildProperties: BuildProperties,
  @Value("\${api.base.url.issuer-uri}") val issuerUri: String,
) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("/").description("Current url"),
      ),
    )
    .tags(
      listOf(
        Tag().name("01. Acquirer").description("Acquiring events from the platform"),
        Tag().name("02. Supplier").description("Supplying events to the platform"),
        Tag().name("11. Acquirers").description("Maintenance of Acquirers"),
        Tag().name("12. Suppliers").description("Maintenance of Suppliers"),
        Tag().name("13. Admin").description("Admin functions"),
        Tag().name("20. Data").description("Maintenance of Data"),
      ),
    )
    .info(
      Info().title("GDX Data Share API")
        .version(version)
        .license(License().name("MIT").url("https://opensource.org/license/mit-0"))
        .description("API for obtaining data about citizen life events")
        .contact(
          Contact()
            .name("GDX Platform Team")
            .email("gdx-dev-team@digital.cabinet-office.gov.uk")
            .url("https://www.gov.uk/government/organisations/government-digital-service"),
        ),
    )
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      )
        .addSecuritySchemes(
          "cognito",
          SecurityScheme()
            .type(SecurityScheme.Type.OPENIDCONNECT)
            .openIdConnectUrl("$issuerUri/.well-known/openid-configuration"),
        ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))
    .addSecurityItem(SecurityRequirement().addList("cognito"))

  @Bean
  fun openAPICustomiser(): OpenApiCustomizer = OpenApiCustomizer {
    it.components.schemas.forEach { (_, schema: Schema<*>) ->
      val properties = schema.properties ?: mutableMapOf()
      for (propertyName in properties.keys) {
        val propertySchema = properties[propertyName]!!
        if (propertySchema is DateTimeSchema) {
          properties.replace(
            propertyName,
            StringSchema()
              .example("2021-07-05T10:35:17")
              .pattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
              .description(propertySchema.description)
              .required(propertySchema.required),
          )
        }
      }
    }
  }
}

package uk.gov.gdx.datashare.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.*
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

@Configuration
class WebClientConfiguration(
  @Value("\${api.base.url.lev}") private val levApiRootUri: String,
  @Value("\${api.base.url.prisoner-search}") private val prisonerSearchApiUri: String,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun levApiWebClient(): WebClient {
    val connectionProvider = ConnectionProvider.builder("levApi")
      .maxConnections(100)
      .maxIdleTime(Duration.ofSeconds(20))
      .maxLifeTime(Duration.ofSeconds(60))
      .pendingAcquireTimeout(Duration.ofSeconds(60))
      .evictInBackground(Duration.ofSeconds(120))
      .build()
    val httpClient = HttpClient.create(connectionProvider).responseTimeout(Duration.ofSeconds(10))
    return WebClient.builder()
      .baseUrl(levApiRootUri)
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .defaultHeader("X-Auth-Aud", "gdx-data-share")
      .defaultHeader("X-Auth-Username", "gdx-data-share-user")
      .build()
  }

  @Bean
  fun prisonerSearchApiWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("prisoner-search")

    val connectionProvider = ConnectionProvider.builder("prisonerSearch")
      .maxConnections(100)
      .maxIdleTime(Duration.ofSeconds(20))
      .maxLifeTime(Duration.ofSeconds(60))
      .pendingAcquireTimeout(Duration.ofSeconds(60))
      .evictInBackground(Duration.ofSeconds(120))
      .build()

    val httpClient = HttpClient.create(connectionProvider).responseTimeout(Duration.ofMinutes(10))
    return WebClient.builder()
      .baseUrl(prisonerSearchApiUri)
      .clientConnector(ReactorClientHttpConnector(httpClient))
      .filter(oauth2Client)
      .codecs { codecs -> codecs.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }
      .build()
  }

  @Bean
  fun offenderSearchHealthWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(prisonerSearchApiUri)
      .build()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}

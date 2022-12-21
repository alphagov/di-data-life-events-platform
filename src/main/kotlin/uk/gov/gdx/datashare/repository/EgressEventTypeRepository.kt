package uk.gov.gdx.datashare.repository

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface EgressEventTypeRepository : CoroutineCrudRepository<EgressEventType, UUID>

package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.replication

import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.PageMetadata
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.paged.PagedResponse
import java.util.UUID

class IdSearchResponse(
  override val content: List<IdResponse>,
  override val metadata: PageMetadata,
) : PagedResponse<IdResponse>

data class IdResponse(val id: UUID)

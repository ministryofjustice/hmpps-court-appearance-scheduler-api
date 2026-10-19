package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import java.text.Normalizer

@Entity
@Table(name = "court_code")
final class CourtCode(
  @Id
  @Column(name = "code", nullable = false, length = Integer.MAX_VALUE)
  val code: String,
  description: String,
) {
  @Column(name = "sort", nullable = false)
  var sort: ByteArray = description.asSort()
    private set

  fun applySort(description: String) = apply {
    sort = description.asSort()
  }

  companion object {
    private val DIA_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+}")
    private val NON_ALPHA_REGEX = Regex("[^a-z0-9+]")

    private fun String.asSort() = NON_ALPHA_REGEX.replace(
      DIA_REGEX.replace(
        Normalizer.normalize(this, Normalizer.Form.NFD).lowercase(),
        "",
      ),
      "",
    ).toByteArray(Charsets.UTF_8)
  }
}

interface CourtCodeRepository : JpaRepository<CourtCode, String>

package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.domain

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.model.ExternalReference

@Converter
class ExternalReferenceConverter : AttributeConverter<ExternalReference, String> {
  override fun convertToDatabaseColumn(attribute: ExternalReference?): String? = attribute?.toString()
  override fun convertToEntityAttribute(dbData: String?): ExternalReference? = dbData?.let { ExternalReference.fromString(it) }
}

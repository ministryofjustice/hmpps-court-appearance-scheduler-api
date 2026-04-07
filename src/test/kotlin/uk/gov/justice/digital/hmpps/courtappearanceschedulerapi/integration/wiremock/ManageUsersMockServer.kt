package uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.http.HttpStatus
import tools.jackson.module.kotlin.jsonMapper
import uk.gov.justice.digital.hmpps.courtappearanceschedulerapi.integration.manageusers.UserDetails

class ManageUsersServer : WireMockServer(8095) {

  fun givenUser(username: String, userDetails: UserDetails = user(username), status: HttpStatus = HttpStatus.OK) {
    val response = aResponse().withHeader("Content-Type", "application/json")
    if (status == HttpStatus.OK) {
      response.withBody(jsonMapper().writeValueAsString(userDetails))
    }
    stubFor(get("/users/$username").willReturn(response.withStatus(status.value())))
  }

  companion object {
    fun user(username: String, name: String = username) = UserDetails(username, name)
  }
}

class ManageUsersExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val manageUsers = ManageUsersServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    manageUsers.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageUsers.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    manageUsers.stop()
  }
}

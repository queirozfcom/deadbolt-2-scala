package be.objectify.deadbolt.scala.views.subjectTest

import be.objectify.deadbolt.scala.views.AbstractViewTest
import be.objectify.deadbolt.scala.views.html.subjectTest.subjectPresentOrContent
import play.api.test.{FakeRequest, Helpers, WithApplication}

/**
 * @author Steve Chaloner (steve@objectify.be)
 */
class SubjectPresentOrTest extends AbstractViewTest {

  "show constrained content and hide fallback content when subject is present" in new WithApplication(testApp(handler(subject = Some(user())))) {
    val html = subjectPresentOrContent(FakeRequest())

    private val content: String = Helpers.contentAsString(html)
    content must contain("This is before the constraint.")
    content must contain("This is protected by the constraint.")
    content must not contain("This is default content in case the constraint denies access to the protected content.")
    content must contain("This is after the constraint.")
  }

  "hide constrained content and show fallback content when subject is not present" in new WithApplication(testApp(handler())) {
    val html = subjectPresentOrContent(FakeRequest())

    private val content: String = Helpers.contentAsString(html)
    content must contain("This is before the constraint.")
    content must not contain("This is protected by the constraint.")
    content must contain("This is default content in case the constraint denies access to the protected content.")
    content must contain("This is after the constraint.")
  }
}

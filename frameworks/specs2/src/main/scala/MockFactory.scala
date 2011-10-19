package com.borachio.specs2

import com.borachio.{ MockFactoryBase, MockingURLClassLoader }
import org.specs2.specification.{ BeforeExample, AfterExample, SpecificationStructure, BeforeAfterEach, Fragments }
import org.specs2.mutable.Specification;
import java.net.URL;

//if tweaking this you may want to look at the robospecs project where very similar things were done to get specs2 working with the robolectric, android testing, framework:
//it would be nice if this could be made compatible with robospecs (I could imagine the classloaders conflicting if not architected correctly)
//http://groups.google.com/group/specs2-users/browse_thread/thread/55e2570e824aa35d
//https://github.com/jbrechtel/robospecs

/** A trait that can be mixed into a [[http://etorreborre.github.com/specs2/ Specs2]] specification to provide
  * mocking support.
  */
//trait MockFactory extends MockFactoryBase with BeforeExample with AfterExample {
//  protected def before = resetExpectations
//  protected def after = if (autoverify) verifyExpectations
//  protected var autoverify = true
//}

trait MockSpecs extends Specification with MockSpecsWithInstrumentation {
  lazy val clazz = Class.forName(getClass.getName, true, MockSpecs.mockingClassLoader)
  // lazy val instrumentedClass = MockSpecs.mockingClassLoader.bootstrap(this.getClass)
  //lazy val instrumentedInstance = instrumentedClass.newInstance.asInstanceOf[MockSpecsWithInstrumentation]
  lazy val withMocks = clazz.newInstance.asInstanceOf[MockSpecsWithInstrumentation]
  def instrumentedFragments = super.is
  override def is = withMocks.setup(withMocks.mockedFragments)
}

trait MockAcceptanceSpecs extends org.specs2.Specification with MockSpecsWithInstrumentation {
  lazy val clazz = Class.forName(getClass.getName, true, MockSpecs.mockingClassLoader)
  lazy val withMocks = clazz.newInstance.asInstanceOf[MockSpecsWithInstrumentation]
  MockSpecs.mockingClassLoader.factory.set(withMocks)
  override def map(f: => Fragments) = withMocks.setup(withMocks.is)
  def instrumentedFragments = is
}

trait MockSpecsWithInstrumentation extends MockFactoryBase with SpecificationStructure {
  lazy val setup = new BeforeAfterEach {
    def before { resetExpectations }
    def ^(fs: Fragments) = this(fs)
    def after = if (autoverify) verifyExpectations
    var autoverify = true
  }

  def mockedFragments: Fragments
  //lazy val robolectricConfig = new RobolectricConfig(new File("./src/main"))
  //lazy val resourceLoader = {
  // val rClassName: String = robolectricConfig.getRClassName
  // val rClass: Class[_] = Class.forName(rClassName)
  // new ResourceLoader(robolectricConfig.getSdkVersion, rClass, robolectricConfig.getResourceDirectory, robolectricConfig.getAssetsDirectory)
  //}

  //  def setupApplicationState() {
  //    robolectricConfig.validate()
  //    Robolectric.bindDefaultShadowClasses()
  //    Robolectric.resetStaticState()
  //    Robolectric.application = ShadowApplication.bind(new ApplicationResolver(robolectricConfig).resolveApplication, resourceLoader)
  //  }
}

object MockSpecs {
  def envHasMockClasses = System.getenv().containsKey("mock.classes")
  if (!envHasMockClasses) throw new RuntimeException("borachio-sbt-plugin is not loaded!");
  lazy val mockClassesFolder = System.getProperty("mock.classes")

  val mockingClassLoader = new MockingURLClassLoader(new URL("file://" + mockClassesFolder + "/"))

  //no idea how I should load this class...in Robospecs we used Javaassist delegateloadingof
  mockingClassLoader.loadClassNormal(classOf[MockSpecsWithInstrumentation].getName);
  // val runInternal = clazz.getMethod("runInternal", classOf[Option[String]], classOf[Reporter], classOf[Stopper],
  //   classOf[Filter], classOf[Map[String, Any]], classOf[Option[Distributor]], classOf[Tracker])
  //runInternal.invoke(withMocks, testName, reporter, stopper, filter, configMap, distributor, tracker)
  // } else {
  //runInternal(testName, reporter, stopper, filter, configMap, distributor, tracker)
  //}
  //  lazy val classLoader = {
  //    loader.delegateLoadingOf("org.specs2.")
  //    loader.delegateLoadingOf("scala.")
  //    loader.delegateLoadingOf(classOf[RoboSpecsWithInstrumentation].getName)
  //
  //    loader
  //  }

}


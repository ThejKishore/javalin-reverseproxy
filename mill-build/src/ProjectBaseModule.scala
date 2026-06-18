package millbuild

import mill.*
import mill.javalib.*

trait ProjectBaseModule extends MavenModule {

  def depManagement = Seq(mvn"org.apache.commons:commons-text:1.13.0")
  def javacOptions = Seq("-source", "21", "-target", "21")

  trait ProjectBaseTests extends MavenTests with TestModule.Junit5 {

    def forkWorkingDir = moduleDir
    def javacOptions = Seq("-source", "21", "-target", "21")
    def testParallelism = false
    def testSandboxWorkingDir = false

  }

}

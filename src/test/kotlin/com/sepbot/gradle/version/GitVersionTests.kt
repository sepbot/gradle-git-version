package com.sepbot.gradle.version

import org.gradle.internal.impldep.org.junit.Rule
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.*
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GitVersionTests {

  @Rule private val dir = TemporaryFolder()
  private val classpath: List<File>

  init {
    val manifest = javaClass.classLoader.getResource("plugin-classpath.txt")
        ?: throw IllegalStateException("Did not find plugin-classpath.txt")
    classpath = manifest.readText().lines().map { File(it) }
  }

  @BeforeEach
  fun setup() {
    dir.create()
    dir.newFile("build.gradle").writeText("""
      plugins { id 'git-version' }
      version gitVersion.default
    """.trimIndent())
  }

  @AfterEach
  fun teardown() {
    dir.delete()
  }

  @Test
  fun withoutGit() {
    val result = build("version")
    assertVersion(result, "unversioned")
  }

  @Test
  fun withoutCommits() {
    GitClient.execute(dir.root, "init")
    val result = build("version")
    assertVersion(result, "unversioned")
  }

  @Test
  fun cleanCommitWithoutTag() {
    GitClient.execute(dir.root, "init")
    dir.newFile("test")
    GitClient.execute(dir.root, "add", "test")
    GitClient.execute(dir.root, "commit", "-m", "test")
    val result = build("version")
    assertVersion(result, "unversioned")
  }

  @Test
  fun dirtyCommitWithoutTag() {
    GitClient.execute(dir.root, "init")
    val file = dir.newFile("test")
    GitClient.execute(dir.root, "add", "test")
    GitClient.execute(dir.root, "commit", "-m", "test")
    file.writeText("test")
    val result = build("version")
    assertVersion(result, "unversioned-SNAPSHOT")
  }

  private fun build(vararg args: String): BuildResult {
    return GradleRunner.create()
        .withProjectDir(dir.root)
        .withPluginClasspath(classpath)
        .withArguments("--stacktrace", *args)
        .build()
  }

  private fun assertVersion(result: BuildResult, version: String) {
    val lines = result.output.trim().split(System.lineSeparator())
    val actual = lines[lines.indexOf(":version") + 1]
    Assertions.assertEquals(version, actual)
  }

}

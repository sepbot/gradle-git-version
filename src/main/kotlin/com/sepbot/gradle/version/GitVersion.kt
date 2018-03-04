package com.sepbot.gradle.version

import org.gradle.api.Plugin
import org.gradle.api.Project

class GitVersion: Plugin<Project> {

  override fun apply(target: Project) {
    target.extensions.create("gitVersion", GitVersionExtension::class.java)
    target.tasks.create("version") {
      it.doLast { println(target.version) }
    }
  }

}

@Suppress("MemberVisibilityCanBePrivate")
open class GitVersionExtension {
  val default: String
  val tag: String?
  val commit: String?
  val onTag: Boolean
  val modified: List<String>
  val notTracked: List<String> = listOf()


  init {
    val tagResult = GitClient.execute("describe", "--abbrev=0", "--tags")
    val fullCommitResult = GitClient.execute("rev-parse", "HEAD")
    val commitResult = GitClient.execute("rev-parse", "--short", "HEAD")
    val modifiedResult = GitClient.execute("ls-files", "-m")
    tag = if (tagResult.code == 0) { tagResult.output } else { null }
    commit = if (commitResult.code == 0) { commitResult.output } else { null }
    onTag = if (tagResult.code == 0 && fullCommitResult.code == 0) {
      val tagCommit = GitClient.execute("rev-list", "-n", "1", tagResult.output)
      if (tagCommit.code == 0) { tagCommit.output == fullCommitResult.output } else { false }
    } else {
      false
    }
    modified = if (modifiedResult.code == 0) {
      modifiedResult.output.split(System.lineSeparator())
    } else {
      listOf()
    }
    default = buildString {
      if (tag == null) {
        append("unversioned")
      } else {
        append(tag)
      }
      if (!onTag && commit != null) {
        append("-$commit")
      }
      if (modified.isNotEmpty()) {
        append("-SNAPSHOT")
      }
    }
  }

}

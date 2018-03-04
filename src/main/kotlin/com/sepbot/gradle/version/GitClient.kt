package com.sepbot.gradle.version

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

internal class GitClientException(override val message: String): RuntimeException(message)

internal data class GitCommandResult(val code: Int, val output: String)

internal object GitClient {
  private val verified = AtomicBoolean(false)

  fun execute(cmd: String, vararg args: String): GitCommandResult {
    return execute(rootDir(File(".")), cmd, *args)
  }

  fun execute(working: File, cmd: String, vararg args: String): GitCommandResult {
    if (!verified.getAndSet(true)) {
      if (ProcessBuilder("git", "version").start().waitFor() != 0) {
        throw GitClientException("git command not available")
      }
    }
    val builder = ProcessBuilder("git", cmd, *args)
    builder.directory(working)
    builder.redirectErrorStream(true)
    val process = builder.start()
    val code = process.waitFor()
    val output = BufferedReader(InputStreamReader(process.inputStream)).useLines {
      it.joinToString(separator = System.lineSeparator()).trim()
    }
    return GitCommandResult(code, output)
  }

  private tailrec fun rootDir(parent: File): File {
    val git = parent.resolve(".git")
    return when {
      git.isDirectory -> git
      parent.parentFile.exists() -> GitClient.rootDir(parent.parentFile)
      else -> throw GitClientException(".git directory not found")
    }
  }

}

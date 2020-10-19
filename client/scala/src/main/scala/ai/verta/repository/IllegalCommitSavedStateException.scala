package ai.verta.repository

final case class IllegalCommitSavedStateException(
  private val message: String = "",
  private val cause: Throwable = None.orNull
) extends Exception(message, cause)

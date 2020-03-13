package ai.verta.swagger.client

import sttp.model.StatusCode

final case class HttpException(code: StatusCode,
                               message: String,
                               cause: Throwable = None.orNull)
  extends Exception(s"[$code] $message", cause)
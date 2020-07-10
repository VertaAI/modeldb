package ai.verta.swagger.client

import java.io.InputStream
import java.net.{URI, URLEncoder}

import ai.verta.swagger.client.objects.BaseSwagger
import net.liftweb.json.{DefaultFormats, JValue, compactRender, parse}
import sttp.client._
import sttp.client.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.model._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import net.liftweb.json._
import net.liftweb.json.Serialization.write

class HttpClient(val host: String, val headers: Map[String, String]) {
  implicit val formats = DefaultFormats
  implicit val sttpBackend = AsyncHttpClientFutureBackend()

  private def urlEncodeUTF8(s: String) = {
    URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20")
  }

  private def urlEncodeUTF8(q: Map[String, List[String]]): String = {
    if (q.isEmpty) "" else
      q
        .flatMap(entry => entry._2.map(x => urlEncodeUTF8(entry._1) + "=" + urlEncodeUTF8(x)))
        .reduce((p1, p2) => p1 + "&" + p2)
  }

  def request[T1, T2](method: String, path: String, query: Map[String, List[String]], body: T1, parser: JValue => T2)(implicit ec: ExecutionContext, m: Manifest[T2]): Future[Try[T2]] = {
    val safePath = path.split("/").map(urlEncodeUTF8).mkString("/")

    if (body == null)
      requestInternal(method, safePath, query, null, parser)
    else
      body match {
        case b: BaseSwagger => requestInternal(method, safePath, query, compactRender(b.toJson()), parser)
        case b: String => requestInternal(method, safePath, query, jsonFormat(b), parser)
      }
  }

  def requestInternal[T2](method: String, path: String, query: Map[String, List[String]], body: String, parser: JValue => T2)(implicit ec: ExecutionContext, m: Manifest[T2]): Future[Try[T2]] = {
    val request = if (body != null) basicRequest.body(body) else basicRequest

    val queryString = urlEncodeUTF8(query)
    val uriPath = if (query.isEmpty) Uri(new URI(host + path)) else Uri(new URI(host + path + "?" + queryString))

    val request2 = method match {
      case "GET" => request.get(uriPath)
      case "POST" => request.post(uriPath)
      case "PUT" => request.put(uriPath)
      case "DELETE" => request.delete(uriPath)
      case _ => throw new IllegalArgumentException(s"unknown method $method") // TODO: wrap around a Try
    }

    val futureResponse = request2.headers(headers).send()

    futureResponse.map(response => {
      response.body match {
        case Left(failureBody) => Failure(HttpException(response.code, failureBody))
        case Right(successBody) => {
          val json = parse(successBody)
          val result = parser(json)
          Success(result)
        }
      }
    })
  }

  /** Make a (raw) request, returning the body of the response
   *  @param method method of the request
   *  @param url url of the request
   *  @param query query parameters
   *  @param localHeaders local headers
   *  @param body body of the request
   *  @return Response, if the request suceeds, wrap in a Future
   */
  def requestRaw(method: String, url: String, query: Map[String, List[String]], localHeaders: Map[String, String], body: InputStream)(implicit ec: ExecutionContext) = {
    val request = (if (body != null) basicRequest.body(body) else basicRequest).response(asByteArray)
    val uriPath = Uri(new URI(url))
    val request2 = method match {
      case "GET" => request.get(uriPath)
      case "POST" => request.post(uriPath)
      case "PUT" => request.put(uriPath)
      case "DELETE" => request.delete(uriPath)
      case _ => throw new IllegalArgumentException(s"unknown method $method")
    }

    (if (localHeaders != null && localHeaders.nonEmpty) request2.headers(localHeaders) else request2).send()
      .map(response => response.body match {
        case Left(failureBody) => Failure(HttpException(response.code, failureBody))
        case Right(successBody) => Success(RawRequestResponse(body = successBody, headers = response.header(_)))
      })
  }

  def toQuery[T](value: T): List[String] = value match {
    case Nil => List()
    case head :: rest => (head :: rest).map(_.toString)
    case _ => List(value.toString)
  }

  def close(): Unit = Await.result(sttpBackend.close(), Duration.Inf)

  /** Utility method to format the string for JSON parsing
   *  @param input input
   */
  private def jsonFormat(input: String) = {
    implicit val formats = DefaultFormats
    write(input)
  }
}

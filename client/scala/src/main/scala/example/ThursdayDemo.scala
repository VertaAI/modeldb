// package example
//
// import ai.verta.client._
// import ai.verta.repository._
//
// import scala.concurrent.ExecutionContext
// import scala.util.Try
//
// object ThursdayDemo extends App {
//   implicit val ec = ExecutionContext.global
//
//   val client = new Client(ClientConnection.fromEnvironment())
//   try {
//     val repo = client.getOrCreateRepository("DemoRepository").get
//
//     val commit = repo.getCommitByBranch()
//     commit.update("abc/cde", PathBlob(""))
//   } finally {
//     client.close()
//   }
// }

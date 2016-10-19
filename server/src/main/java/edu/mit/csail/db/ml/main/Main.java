package edu.mit.csail.db.ml.main;

import edu.mit.csail.db.ml.conf.ModelDbConfig;
import edu.mit.csail.db.ml.server.ModelDbServer;
import modeldb.ModelDBService;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportFactory;

public class Main {
  public static void main(String[] args) throws Exception {
    ModelDbConfig config = ModelDbConfig.parse(args);
    try {
      TServerTransport transport = new TServerSocket(config.thriftPort);
      TProtocolFactory protocolFactory = new TBinaryProtocol.Factory();
      TTransportFactory transportFactory = new TFramedTransport.Factory();
      TThreadPoolServer.Args serverArgs = new TThreadPoolServer.Args(transport)
        .processor(new ModelDBService.Processor<ModelDbServer>(new ModelDbServer(
          config.dbUser,
          config.dbPassword,
          config.jbdcUrl,
          config.dbType
        )))
        .protocolFactory(protocolFactory)
        .transportFactory(transportFactory)
        .minWorkerThreads(1)
        .maxWorkerThreads(100);
      TThreadPoolServer server = new TThreadPoolServer(serverArgs);

      System.out.printf("Starting the simple server on port %d...\n", config.thriftPort);
      server.serve();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
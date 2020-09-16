/*
 * Copyright 2016 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified class io.grpc.services.HealthServiceImpl
 * Verta changes: check ready status in uacservice
 */

package ai.verta.modeldb.health;

import ai.verta.modeldb.ModelDBConstants;
import ai.verta.modeldb.monitoring.QPSCountResource;
import ai.verta.modeldb.monitoring.RequestLatencyResource;
import ai.verta.modeldb.utils.ModelDBHibernateUtil;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class HealthServiceImpl extends HealthGrpc.HealthImplBase {

  /* Due to the latency of rpc calls, synchronization of the map does not help with consistency.
   * However, need use ConcurrentHashMap to prevent the possible race condition of concurrently
   * putting two keys with a colliding hashCode into the map.*/
  private final Map<String, ServingStatus> statusMap = new ConcurrentHashMap<>();
  private static final Logger LOGGER = LogManager.getLogger(HealthServiceImpl.class);

  @Override
  public void check(
      HealthCheckRequest request, StreamObserver<HealthCheckResponse> responseObserver) {
    ServingStatus status = getServingStatus(request);
    if (status == null) {
      responseObserver.onError(
          new StatusException(
              Status.NOT_FOUND.withDescription("unknown service " + request.getService())));
    } else {
      HealthCheckResponse response = HealthCheckResponse.newBuilder().setStatus(status).build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }
  }

  private ServingStatus getServingStatus(HealthCheckRequest request) {
    ServingStatus globalStatus;
    if (!request.getService().equals("")) {
      globalStatus = getStatus("");
      if (request.getService().equals("ready")) {
        if (globalStatus == ServingStatus.SERVING) {
          setStatus("ready", ModelDBHibernateUtil.checkReady());
        }
      } else if (request.getService().equals("live")) {
        setStatus("live", ModelDBHibernateUtil.checkLive());
      }
    }
    return getStatus(request.getService());
  }

  void setStatus(String service, ServingStatus status) {
    statusMap.put(service, status);
  }

  @Nullable
  private ServingStatus getStatus(String service) {
    return statusMap.get(service);
  }

  void clearStatus(String service) {
    statusMap.remove(service);
  }

  @GetMapping(value = {ModelDBConstants.SPRING_HEALTH_CHECK_METHOD_NAME})
  public ResponseEntity<String> check(
      @RequestParam(ModelDBConstants.HEALTH_CHECK_SERVICE_FIELD) String service) {
    LOGGER.trace("Spring custom health check called");
    QPSCountResource.inc();
    try (RequestLatencyResource latencyResource =
        new RequestLatencyResource(ModelDBConstants.SPRING_HEALTH_CHECK_METHOD_NAME)) {
      HealthCheckRequest request = HealthCheckRequest.newBuilder().setService(service).build();
      ServingStatus status = getServingStatus(request);
      if (status == null) {
        LOGGER.trace("Spring custom health check returned with unknown service");
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Unknown service found on health check request : '" + request.getService() + "'");
      } else {
        LOGGER.trace("Spring custom health check returned");
        HealthCheckResponse response = HealthCheckResponse.newBuilder().setStatus(status).build();
        return ResponseEntity.ok(response.toString());
      }
    }
  }
}

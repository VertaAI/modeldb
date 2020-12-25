package ai.verta.modeldb.lineage;

import static org.mockito.Mockito.doNothing;

import ai.verta.modeldb.AddLineage;
import ai.verta.modeldb.DeleteLineage;
import ai.verta.modeldb.FindAllInputs;
import ai.verta.modeldb.FindAllInputsOutputs;
import ai.verta.modeldb.FindAllOutputs;
import ai.verta.modeldb.LineageEntry;
import ai.verta.modeldb.LineageEntryEnum.LineageEntryType;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LineageServiceImplNegativeTest {

  @Mock private LineageDAO lineageDAO;
  @Mock private StreamObserver<AddLineage.Response> addLineageObserver;
  @Mock private StreamObserver<DeleteLineage.Response> deleteLineageObserver;
  @Mock private StreamObserver<FindAllInputs.Response> findAllInputsObserver;
  @Mock private StreamObserver<FindAllOutputs.Response> findAllOutputsObserver;
  @Mock private StreamObserver<FindAllInputsOutputs.Response> findAllInputOutputsObserver;
  private LineageServiceImpl sut;
  @Captor private ArgumentCaptor<Throwable> captorThrow;

  @Before
  public void before() {
    sut = new LineageServiceImpl(lineageDAO, null, null);
  }

  @Test
  public void addLineage() {
    doNothing().when(addLineageObserver).onError(captorThrow.capture());
    sut.addLineage(AddLineage.newBuilder().build(), addLineageObserver);
    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) captorThrow.getValue();
    String description = statusRuntimeException.getStatus().getDescription();
    assert description != null;
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("input"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("output"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("not"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("specified"));

    sut.addLineage(
        AddLineage.newBuilder()
            .addInput(
                LineageEntry.newBuilder()
                    .setExternalId("123")
                    .setType(LineageEntryType.DATASET_VERSION))
            .build(),
        addLineageObserver);
    statusRuntimeException = (StatusRuntimeException) captorThrow.getValue();
    description = statusRuntimeException.getStatus().getDescription();
    assert description != null;
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("output"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("not"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("specified"));

    sut.addLineage(
        AddLineage.newBuilder()
            .addOutput(
                LineageEntry.newBuilder()
                    .setExternalId("123")
                    .setType(LineageEntryType.DATASET_VERSION))
            .build(),
        addLineageObserver);
    statusRuntimeException = (StatusRuntimeException) captorThrow.getValue();
    description = statusRuntimeException.getStatus().getDescription();
    assert description != null;
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("input"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("not"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("specified"));
  }

  @Test
  public void deleteLineage() {
    doNothing().when(deleteLineageObserver).onError(captorThrow.capture());
    sut.deleteLineage(DeleteLineage.newBuilder().build(), deleteLineageObserver);
    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) captorThrow.getValue();
    String description = statusRuntimeException.getStatus().getDescription();
    assert description != null;
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("input"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("output"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("not"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("specified"));

    sut.deleteLineage(
        DeleteLineage.newBuilder()
            .addInput(
                LineageEntry.newBuilder()
                    .setExternalId("123")
                    .setType(LineageEntryType.DATASET_VERSION))
            .build(),
        deleteLineageObserver);
    statusRuntimeException = (StatusRuntimeException) captorThrow.getValue();
    description = statusRuntimeException.getStatus().getDescription();
    assert description != null;
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("output"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("not"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("specified"));

    sut.deleteLineage(
        DeleteLineage.newBuilder()
            .addOutput(
                LineageEntry.newBuilder()
                    .setExternalId("123")
                    .setType(LineageEntryType.DATASET_VERSION))
            .build(),
        deleteLineageObserver);
    statusRuntimeException = (StatusRuntimeException) captorThrow.getValue();
    description = statusRuntimeException.getStatus().getDescription();
    assert description != null;
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("input"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("not"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("specified"));
  }

  @Test
  public void findAllInputs() {
    doNothing().when(findAllInputsObserver).onError(captorThrow.capture());
    sut.findAllInputs(FindAllInputs.newBuilder().build(), findAllInputsObserver);
    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) captorThrow.getValue();
    String description = statusRuntimeException.getStatus().getDescription();
    assert description != null;
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("items"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("not"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("specified"));
  }

  @Test
  public void findAllOutputs() {
    doNothing().when(findAllOutputsObserver).onError(captorThrow.capture());
    sut.findAllOutputs(FindAllOutputs.newBuilder().build(), findAllOutputsObserver);
    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) captorThrow.getValue();
    String description = statusRuntimeException.getStatus().getDescription();
    assert description != null;
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("items"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("not"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("specified"));
  }

  @Test
  public void findAllInputsOutputs() {
    doNothing().when(findAllInputOutputsObserver).onError(captorThrow.capture());
    sut.findAllInputsOutputs(
        FindAllInputsOutputs.newBuilder().build(), findAllInputOutputsObserver);
    StatusRuntimeException statusRuntimeException = (StatusRuntimeException) captorThrow.getValue();
    String description = statusRuntimeException.getStatus().getDescription();
    assert description != null;
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("items"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("not"));
    Assert.assertThat(description.toLowerCase(), CoreMatchers.containsString("specified"));
  }
}

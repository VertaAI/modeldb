package ai.verta.modeldb;

import ai.verta.common.CollaboratorTypeEnum.CollaboratorType;
import ai.verta.common.EntitiesEnum.EntitiesTypes;
import ai.verta.uac.AddCollaboratorRequest;
import ai.verta.uac.CollaboratorPermissions;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class CollaboratorUtils {
  private CollaboratorUtils() {}

  public static AddCollaboratorRequest addCollaboratorRequestDataset(
      Dataset dataset, String email, CollaboratorType collaboratorType) {
    return addCollaboratorRequestUser(
        dataset.getId(), email, collaboratorType, "Please refer shared dataset for your invention");
  }

  public static AddCollaboratorRequest addCollaboratorRequestProjectInterceptor(
      Project project,
      CollaboratorType collaboratorType,
      AuthClientInterceptor authClientInterceptor0) {
    return addCollaboratorRequestProject(
        project, authClientInterceptor0.getClient2Email(), collaboratorType);
  }

  public static AddCollaboratorRequest addCollaboratorRequestProjectInterceptor(
      List<String> ids,
      CollaboratorType collaboratorType,
      AuthClientInterceptor authClientInterceptor0) {
    return addCollaboratorRequest(ids, authClientInterceptor0.getClient2Email(), collaboratorType);
  }

  public static AddCollaboratorRequest addCollaboratorRequest(
      List<String> ids, String shareWith, CollaboratorType collaboratorType) {
    return addCollaboratorRequestUser(
        ids, shareWith, collaboratorType, "Please refer shared project for your invention");
  }

  static AddCollaboratorRequest addCollaboratorRequestProject(
      Project project, String email, CollaboratorType collaboratorType) {
    return addCollaboratorRequest(
        Collections.singletonList(project.getId()), email, collaboratorType);
  }

  static AddCollaboratorRequest addCollaboratorRequestUser(
      String entityId, String email, CollaboratorType collaboratorType, String message) {
    return addCollaboratorRequestUser(
        Collections.singletonList(entityId), email, collaboratorType, message);
  }

  static AddCollaboratorRequest addCollaboratorRequestUser(
      List<String> ids, String email, CollaboratorType collaboratorType, String message) {
    return AddCollaboratorRequest.newBuilder()
        .addAllEntityIds(ids)
        .setShareWith(email)
        .setAuthzEntityType(EntitiesTypes.USER)
        .setPermission(
            CollaboratorPermissions.newBuilder().setCollaboratorType(collaboratorType).build())
        .setDateCreated(Calendar.getInstance().getTimeInMillis())
        .setMessage(message)
        .build();
  }
}

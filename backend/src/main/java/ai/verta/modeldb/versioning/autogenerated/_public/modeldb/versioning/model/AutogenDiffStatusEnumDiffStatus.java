// THIS FILE IS AUTO-GENERATED. DO NOT EDIT
package ai.verta.modeldb.versioning.autogenerated._public.modeldb.versioning.model;

import ai.verta.modeldb.common.exceptions.ModelDBException;
import ai.verta.modeldb.versioning.*;
import ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus;
import ai.verta.modeldb.versioning.blob.diff.ProtoType;
import ai.verta.modeldb.versioning.blob.visitors.Visitor;
import java.util.*;

public class AutogenDiffStatusEnumDiffStatus implements ProtoType {

  public DiffStatusEnum.DiffStatus Status;

  public AutogenDiffStatusEnumDiffStatus() {
    Status = DiffStatus.UNKNOWN;
  }

  public AutogenDiffStatusEnumDiffStatus(DiffStatusEnum.DiffStatus s) {
    Status = s;
  }

  public Boolean isEmpty() {
    return Status == DiffStatus.UNKNOWN;
  }

  public Boolean isDeleted() {
    return Status == DiffStatus.UNKNOWN || Status == DiffStatus.DELETED;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (!(o instanceof AutogenDiffStatusEnumDiffStatus)) {
      return false;
    }
    AutogenDiffStatusEnumDiffStatus other = (AutogenDiffStatusEnumDiffStatus) o;
    return Status == other.Status;
  }

  public static AutogenDiffStatusEnumDiffStatus fromProto(
      ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus blob) {
    if (blob == null) {
      return null;
    }

    AutogenDiffStatusEnumDiffStatus obj = new AutogenDiffStatusEnumDiffStatus();
    obj.Status = blob;
    return obj;
  }

  public ai.verta.modeldb.versioning.DiffStatusEnum.DiffStatus toProto() {
    return this.Status;
  }

  @Override
  public String toString() {
    return Status.toString();
  }

  public void preVisitShallow(Visitor visitor) throws ModelDBException {
    visitor.preVisitAutogenDiffStatusEnumDiffStatus(this);
  }

  public void preVisitDeep(Visitor visitor) throws ModelDBException {
    this.preVisitShallow(visitor);
  }

  public AutogenDiffStatusEnumDiffStatus postVisitShallow(Visitor visitor) throws ModelDBException {
    return visitor.postVisitAutogenDiffStatusEnumDiffStatus(this);
  }

  public AutogenDiffStatusEnumDiffStatus postVisitDeep(Visitor visitor) throws ModelDBException {
    return this.postVisitShallow(visitor);
  }
}

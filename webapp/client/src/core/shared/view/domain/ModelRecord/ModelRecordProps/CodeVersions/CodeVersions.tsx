import * as React from 'react';
import { NavLink } from 'react-router-dom';
import { connect } from 'react-redux';

import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import ModelRecord, {
  ICodeVersionsFromBlob,
  IVersionedInputs,
} from 'core/shared/models/ModelRecord';
import { ICodeVersion } from 'core/shared/models/CodeVersion';
import CodeVersion from 'core/shared/view/domain/CodeVersion/CodeVersion';
import routes from 'routes';
import { CommitPointerHelpers } from 'core/shared/models/Versioning/RepositoryData';
import * as CommitComponentLocation from 'core/shared/models/Versioning/CommitComponentLocation';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import styles from './CodeVersions.module.css';

const mapStateToProps = (state: IApplicationState) => {
  return {
    workspaceName: selectCurrentWorkspaceName(state),
  };
};

type AllProps = {
  experimentRunId: ModelRecord['id'];
  versionedInputs?: IVersionedInputs;
  codeVersion?: ICodeVersion;
  codeVersionsFromBlob?: ICodeVersionsFromBlob;
} & ReturnType<typeof mapStateToProps>;

const CodeVersions = ({
  versionedInputs,
  experimentRunId,
  codeVersion,
  codeVersionsFromBlob,
  workspaceName,
}: AllProps) => {
  return (
    <ScrollableContainer
      maxHeight={180}
      containerOffsetValue={12}
      children={
        <>
          {codeVersion && (
            <CodeVersion
              entityId={experimentRunId}
              entityType="experimentRun"
              codeVersion={codeVersion}
            />
          )}
          {codeVersionsFromBlob &&
            Object.entries(codeVersionsFromBlob).map(
              ([location, codeVersion]) => {
                return (
                  <CodeVersion
                    entityId={experimentRunId}
                    entityType="experimentRun"
                    pileProps={{
                      label: location,
                    }}
                    popupProps={{
                      additionalFields: [
                        {
                          label: 'Location',
                          content: versionedInputs ? (
                            <NavLink
                              className={styles.blobLink}
                              to={routes.repositoryDataWithLocation.getRedirectPath(
                                {
                                  commitPointer: CommitPointerHelpers.makeFromCommitSha(
                                    versionedInputs.commitSha
                                  ),
                                  location: CommitComponentLocation.makeFromPathname(
                                    location
                                  ),
                                  repositoryName:
                                    versionedInputs.repositoryName,
                                  workspaceName,
                                  type: 'blob',
                                }
                              )}
                            >
                              {location}
                            </NavLink>
                          ) : (
                            location
                          ),
                        },
                      ],
                    }}
                    codeVersion={codeVersion}
                  />
                );
              }
            )}
        </>
      }
    />
  );
};

export default connect(mapStateToProps)(CodeVersions);

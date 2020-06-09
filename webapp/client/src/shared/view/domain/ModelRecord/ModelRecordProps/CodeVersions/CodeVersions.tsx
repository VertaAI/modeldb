import * as React from 'react';
import { NavLink } from 'react-router-dom';
import { connect } from 'react-redux';

import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';
import ModelRecord, {
  ICodeVersionsFromBlob,
  IVersionedInputs,
} from 'shared/models/ModelRecord';
import { ICodeVersion } from 'shared/models/CodeVersion';
import CodeVersion from 'shared/view/domain/CodeVersion/CodeVersion';
import routes from 'shared/routes';
import { CommitPointerHelpers } from 'shared/models/Versioning/RepositoryData';
import * as CommitComponentLocation from 'shared/models/Versioning/CommitComponentLocation';
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

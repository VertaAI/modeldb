import * as React from 'react';

import { ICodeVersion } from 'core/shared/models/CodeVersion';
import { EntityType } from 'store/artifactManager';

import ArtifactCodeVersion from './ArtifactCodeVersion/ArtifactCodeVersion';
import styles from './CodeVersion.module.css';
import GitCodeVersionButton from './GitCodeVersionButton/GitCodeVersionButton';

interface ILocalProps {
  entityType: EntityType;
  entityId: string;
  codeVersion: ICodeVersion;
  getGitClassname?: () => string | undefined;
  getArtifactClassname?: () => string | undefined;
}

class CodeVersion extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      codeVersion,
      entityId,
      entityType,
      getGitClassname = () => undefined,
      getArtifactClassname = () => undefined,
    } = this.props;
    return (
      <div className={styles.root} data-test="code-version">
        {codeVersion.type === 'artifact' ? (
          <ArtifactCodeVersion
            additionalButtonClassname={getArtifactClassname()}
            artifactCodeVersion={codeVersion}
            entityId={entityId}
            entityType={entityType}
          />
        ) : (
          <GitCodeVersionButton
            gitCodeVersion={codeVersion}
            additionalClassnameButton={getGitClassname()}
          />
        )}
      </div>
    );
  }
}

export default CodeVersion;

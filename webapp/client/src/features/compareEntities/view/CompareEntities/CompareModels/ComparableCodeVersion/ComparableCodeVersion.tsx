import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import {
  ArtifactCodeVersionPile,
  ArtifactCodeVersionFields,
} from 'core/shared/view/domain/CodeVersion/ArtifactCodeVersion/ArtifactCodeVersion';
import {
  GitCodeVersionPile,
  GitCodeVersionPopupFields,
} from 'core/shared/view/domain/CodeVersion/GitCodeVersionButton/GitCodeVersionButton';
import {
  getDiffValueBorderClassname,
  getDiffValueBgClassname,
} from 'features/compareEntities/view/CompareEntities/shared/DiffHighlight/DiffHighlight';
import {
  ICodeVersion,
  IGitCodeVersion,
  IArtifactCodeVersion,
} from 'core/shared/models/CodeVersion';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import Button from 'core/shared/view/elements/Button/Button';
import PilePopup from 'core/shared/view/elements/PileWithPopup/PilePopup/PilePopup';
import PileWithPopup from 'core/shared/view/elements/PileWithPopup/PileWithPopup/PileWithPopup';
import {
  EntityType,
  CodeVersionDiffInfo,
} from 'features/compareEntities/store';
import { makeCompareCommitsUrl } from 'core/shared/utils/github/github';

import styles from './ComparableCodeVersion.module.css';

interface ICurrentEntityInfo<CodeVersion extends ICodeVersion = ICodeVersion> {
  id: string;
  codeVersion: CodeVersion;
  entityType: EntityType;
  label?: string;
}
interface IOtherEntityInfo<CodeVersion extends ICodeVersion = ICodeVersion> {
  id: string;
  codeVersion?: CodeVersion;
  entityType: EntityType;
  label?: string;
}
interface IEntityInfoWithCodeVersion<
  CodeVersion extends ICodeVersion = ICodeVersion
> {
  id: string;
  codeVersion: CodeVersion;
  entityType: EntityType;
}
type IEntityInfo<
  CodeVersion1 extends ICodeVersion = ICodeVersion,
  CodeVersion2 extends ICodeVersion = ICodeVersion
> = ICurrentEntityInfo<CodeVersion1> | IOtherEntityInfo<CodeVersion2>;

interface IComparedCodeVersions {
  currentEntityInfo: ICurrentEntityInfo;
  otherEntityInfo: IOtherEntityInfo;
  diffInfo: CodeVersionDiffInfo;
}

class ComparableCodeVersion extends React.PureComponent<IComparedCodeVersions> {
  public render() {
    const { currentEntityInfo, diffInfo } = this.props;
    const PileComponent =
      currentEntityInfo.codeVersion.type === 'artifact'
        ? ArtifactCodeVersionPile
        : GitCodeVersionPile;
    const isDifferent = (() => {
      switch (diffInfo.type) {
        case 'artifactCodeVersion':
          return Object.values(diffInfo.diffInfoByKeys).some(v => v);
        case 'diffType':
          return true;
        case 'gitCodeVersion':
          return Object.values(diffInfo.diffInfoByKeys).some(v => v);
        default:
          return exhaustiveCheck(diffInfo, '');
      }
    })();

    return (
      <PileWithPopup
        pileComponent={({ showPopup }) => (
          <PileComponent
            additionalClassname={getDiffValueBorderClassname(
              currentEntityInfo.entityType,
              isDifferent
            )}
            label={currentEntityInfo.label}
            onClick={showPopup}
          />
        )}
        popupComponent={({ closePopup, isOpen }) =>
          isOpen ? (
            <CompareCodeVersionsPopup
              comparedCodeVersions={this.props}
              isOpen={true}
              onClose={closePopup}
            />
          ) : null
        }
      />
    );
  }
}

class CompareCodeVersionsPopup extends React.PureComponent<{
  comparedCodeVersions: IComparedCodeVersions;
  isOpen: boolean;
  onClose(): void;
}> {
  public render() {
    const {
      comparedCodeVersions: { currentEntityInfo, otherEntityInfo },
      isOpen,
      onClose,
    } = this.props;

    const [codeVersionEntity1, codeVersionEntity2] =
      currentEntityInfo.entityType === EntityType.entity1
        ? [currentEntityInfo, otherEntityInfo]
        : [otherEntityInfo, currentEntityInfo];

    return (
      <PilePopup
        isOpen={isOpen}
        title="Comparing code versions"
        titleIcon="codepen"
        width={760}
        onRequestClose={onClose}
      >
        <PilePopup.SplittedContent
          left={this.renderCodeVersionIfExist(codeVersionEntity1)}
          right={this.renderCodeVersionIfExist(codeVersionEntity2)}
        />
        {this.checkAvailableCompareCode(
          currentEntityInfo.codeVersion,
          otherEntityInfo.codeVersion
        ) && (
          <PilePopup.Actions>
            <Button
              dataTest="compare-code-versions-button"
              onClick={this.compareCode}
            >
              Compare Code
            </Button>
          </PilePopup.Actions>
        )}
      </PilePopup>
    );
  }

  @bind
  private renderCodeVersionIfExist(entityInfo: IEntityInfo) {
    return (
      <div
        className={cn(styles.codeVersion, {
          [styles.empty]: !Boolean(entityInfo.codeVersion),
        })}
      >
        {entityInfo.codeVersion
          ? entityInfo.codeVersion.type === 'artifact'
            ? this.renderArtifactCodeVersionFields(
                entityInfo as IEntityInfoWithCodeVersion<IArtifactCodeVersion>
              )
            : this.renderGitCodeVersionFields(
                entityInfo as IEntityInfoWithCodeVersion<IGitCodeVersion>
              )
          : '-'}
      </div>
    );
  }

  @bind
  private renderGitCodeVersionFields(
    entityInfo: IEntityInfoWithCodeVersion<IGitCodeVersion>
  ) {
    const {
      comparedCodeVersions: { diffInfo },
    } = this.props;
    return (
      <GitCodeVersionPopupFields
        getAdditionalValueClassname={field => {
          if (diffInfo.type === 'diffType') {
            return getDiffValueBgClassname(entityInfo.entityType, true);
          }
          if (diffInfo.type === 'gitCodeVersion') {
            return getDiffValueBgClassname(
              entityInfo.entityType,
              diffInfo.diffInfoByKeys[field]
            );
          }
        }}
        gitCodeVersion={entityInfo.codeVersion}
      />
    );
  }

  @bind
  private renderArtifactCodeVersionFields(
    entityInfo: IEntityInfoWithCodeVersion<IArtifactCodeVersion>
  ) {
    const {
      comparedCodeVersions: { diffInfo },
    } = this.props;
    return (
      <>
        <ArtifactCodeVersionFields
          getAdditionalValueClassname={field => {
            if (diffInfo.type === 'diffType') {
              return getDiffValueBgClassname(entityInfo.entityType, true);
            }
            if (diffInfo.type === 'artifactCodeVersion') {
              return getDiffValueBgClassname(
                entityInfo.entityType,
                diffInfo.diffInfoByKeys[field]
              );
            }
          }}
          artifactCodeVersion={entityInfo.codeVersion}
        />
      </>
    );
  }

  @bind
  private checkAvailableCompareCode(
    codeVersion1?: ICodeVersion,
    codeVersion2?: ICodeVersion
  ) {
    if (
      !codeVersion1 ||
      !codeVersion2 ||
      codeVersion1.type !== codeVersion2.type
    ) {
      return false;
    }
    if (codeVersion1.type === 'git' && codeVersion2.type === 'git') {
      return this.checkAvailableCompareGitCodeVersions(
        codeVersion1,
        codeVersion2
      );
    }
    return true;
  }

  @bind
  private checkAvailableCompareGitCodeVersions(
    gitCodeVersion1: IGitCodeVersion,
    gitCodeVersion2: IGitCodeVersion
  ) {
    const components = this.getRepositoryCompareCommitsUrlComponents(
      gitCodeVersion1,
      gitCodeVersion2
    );
    return Boolean(components && makeCompareCommitsUrl(components));
  }
  @bind
  private compareGitCodeVersions(
    gitCodeVersion1: IGitCodeVersion,
    gitCodeVersion2: IGitCodeVersion
  ) {
    const compareCommitsUrlComponents = this.getRepositoryCompareCommitsUrlComponents(
      gitCodeVersion1,
      gitCodeVersion2
    );
    if (compareCommitsUrlComponents) {
      const compareCommitsUrl = makeCompareCommitsUrl(
        compareCommitsUrlComponents
      );
      if (compareCommitsUrl) {
        window.open(compareCommitsUrl, '_blank');
      }
    }
  }

  private getRepositoryCompareCommitsUrlComponents(
    gitCodeVersion1: IGitCodeVersion,
    gitCodeVersion2: IGitCodeVersion
  ) {
    if (
      gitCodeVersion1.data.remoteRepoUrl &&
      gitCodeVersion1.data.remoteRepoUrl.type === 'github' &&
      gitCodeVersion2.data.remoteRepoUrl &&
      gitCodeVersion2.data.remoteRepoUrl.type === 'github' &&
      gitCodeVersion1.data.commitHash &&
      gitCodeVersion2.data.commitHash
    ) {
      return {
        repoWithCommitHash1: {
          url: gitCodeVersion1.data.remoteRepoUrl.value,
          commitHash: gitCodeVersion1.data.commitHash,
        },
        repoWithCommitHash2: {
          url: gitCodeVersion2.data.remoteRepoUrl.value,
          commitHash: gitCodeVersion2.data.commitHash,
        },
      };
    }
  }

  @bind
  private compareCode() {
    const {
      comparedCodeVersions: { currentEntityInfo, otherEntityInfo },
    } = this.props;
    if (currentEntityInfo.codeVersion && otherEntityInfo.codeVersion) {
      if (
        currentEntityInfo.codeVersion.type === 'git' &&
        otherEntityInfo.codeVersion.type === 'git'
      ) {
        this.compareGitCodeVersions(
          currentEntityInfo.codeVersion,
          otherEntityInfo.codeVersion
        );
      } else if (
        currentEntityInfo.codeVersion.type === 'artifact' &&
        otherEntityInfo.codeVersion.type === 'artifact'
      ) {
        const url = `${process.env.REACT_APP_BACKEND_API ||
          ''}/api/v1/nbdiff/query/?base=${currentEntityInfo.id}&remote=${
          otherEntityInfo.id
        }`;
        window.open(url, '_blank');
      }
    }
  }
}

export default ComparableCodeVersion;

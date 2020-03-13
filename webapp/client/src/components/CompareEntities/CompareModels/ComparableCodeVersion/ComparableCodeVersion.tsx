import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import {
  ArtifactCodeVersionPile,
  ArtifactCodeVersionFields,
} from 'components/CodeVersion/ArtifactCodeVersion/ArtifactCodeVersion';
import {
  GitCodeVersionPile,
  GitCodeVersionPopupFields,
} from 'components/CodeVersion/GitCodeVersionButton/GitCodeVersionButton';
import {
  getDiffValueBorderClassname,
  getDiffValueBgClassname,
} from 'components/CompareEntities/shared/DiffHighlight/DiffHighlight';
import {
  ICodeVersion,
  IGitCodeVersion,
  IArtifactCodeVersion,
} from 'core/shared/models/CodeVersion';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import Button from 'core/shared/view/elements/Button/Button';
import PilePopup from 'core/shared/view/elements/PileWithPopup/PilePopup/PilePopup';
import PileWithPopup from 'core/shared/view/elements/PileWithPopup/PileWithPopup/PileWithPopup';
import { EntityType, IModelsDifferentProps } from 'store/compareEntities';

import styles from './ComparableCodeVersion.module.css';

interface ICurrentEntityInfo<CodeVersion extends ICodeVersion = ICodeVersion> {
  id: string;
  codeVersion: CodeVersion;
  entityType: EntityType;
}
interface IOtherEntityInfo<CodeVersion extends ICodeVersion = ICodeVersion> {
  id: string;
  codeVersion?: CodeVersion;
  entityType: EntityType;
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
  diffInfo: IModelsDifferentProps['codeVersion'];
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
      codeVersion1 &&
      codeVersion2 &&
      codeVersion1.type === 'git' &&
      codeVersion2.type === 'git'
    ) {
      return this.checkAvailableCompareGitCodeVersions(
        codeVersion1,
        codeVersion2
      );
    }
  }

  @bind
  private checkAvailableCompareGitCodeVersions(
    gitCodeVersion1: IGitCodeVersion,
    gitCodeVersion2: IGitCodeVersion
  ) {
    return Boolean(
      gitCodeVersion1.data.remoteRepoUrl &&
        gitCodeVersion2.data.remoteRepoUrl &&
        gitCodeVersion1.data.remoteRepoUrl ===
          gitCodeVersion2.data.remoteRepoUrl &&
        gitCodeVersion1.data.commitHash &&
        gitCodeVersion2.data.commitHash
    );
  }
  @bind
  private compareGitCodeVersions(
    gitCodeVersion1: IGitCodeVersion,
    gitCodeVersion2: IGitCodeVersion
  ) {
    const compareCommitsUrl = (() => {
      const [_, userName, repoName] = gitCodeVersion1.data.remoteRepoUrl!.match(
        /git@github\.com\:(.+)\/(.+)\.git/
      );
      const shortCommit1 = gitCodeVersion1.data.commitHash!.slice(0, 6);
      const shortCommit2 = gitCodeVersion2.data.commitHash!.slice(0, 6);
      return `https://github.com/${userName}/${repoName}/compare/${shortCommit1}..${shortCommit2}`;
    })();
    window.open(compareCommitsUrl, '_blank');
  }
  @bind
  private compareCode() {
    const {
      comparedCodeVersions: { currentEntityInfo, otherEntityInfo },
    } = this.props;
    if (
      currentEntityInfo.codeVersion &&
      otherEntityInfo.codeVersion &&
      currentEntityInfo.codeVersion.type === 'git' &&
      otherEntityInfo.codeVersion.type === 'git'
    ) {
      this.compareGitCodeVersions(
        currentEntityInfo.codeVersion,
        otherEntityInfo.codeVersion
      );
    }
  }
}

export default ComparableCodeVersion;

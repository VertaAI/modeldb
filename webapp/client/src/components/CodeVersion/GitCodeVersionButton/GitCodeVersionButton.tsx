import * as React from 'react';

import {
  IGitCodeVersion,
  parseGitRemoteRepoUrl,
} from 'core/shared/models/CodeVersion';
import ExternalLink from 'core/shared/view/elements/ExternalLink/ExternalLink';

import withProps from 'core/shared/utils/react/withProps';
import Pile from 'core/shared/view/elements/PileWithPopup/Pile/Pile';
import PilePopup from 'core/shared/view/elements/PileWithPopup/PilePopup/PilePopup';
import PileWithPopup from 'core/shared/view/elements/PileWithPopup/PileWithPopup/PileWithPopup';

interface ILocalProps {
  gitCodeVersion: IGitCodeVersion;
  additionalClassnameButton?: string;
}

export const GitCodeVersionPile = withProps(Pile)({
  title: 'view git code version',
  dataTest: 'git-code-version',
  label: 'Git Code Version',
  iconType: 'codepen',
});

const GitCodeVersionPopup = ({
  gitCodeVersion,
  isOpen,
  onClose,
}: {
  gitCodeVersion: IGitCodeVersion;
  isOpen: boolean;
  onClose(): void;
}) => {
  return isOpen ? (
    <PilePopup
      title="Git Code Version"
      titleIcon="codepen"
      contentLabel="git-code-version-action"
      isOpen={isOpen}
      onRequestClose={onClose}
    >
      <GitCodeVersionPopupFields gitCodeVersion={gitCodeVersion} />
    </PilePopup>
  ) : null;
};

export const GitCodeVersionPopupFields = ({
  gitCodeVersion,
  getAdditionalValueClassname = () => undefined,
}: {
  getAdditionalValueClassname?: (str: string) => string | undefined;
  gitCodeVersion: IGitCodeVersion;
}) => {
  return (
    <PilePopup.Fields>
      {Field => (
        <>
          {gitCodeVersion.data.commitHash &&
            (gitCodeVersion.data.remoteRepoUrl ? (
              (() => {
                const remoteRepoUrlInfo = parseGitRemoteRepoUrl(
                  gitCodeVersion.data.remoteRepoUrl
                );
                const url = `https://github.com/${remoteRepoUrlInfo.userName}/${
                  remoteRepoUrlInfo.repositoryInfo.name
                }/commit/${gitCodeVersion.data.commitHash}`;
                return (
                  <Field label="Hash">
                    <ExternalLink
                      url={url}
                      text={gitCodeVersion.data.commitHash}
                      additionalClassname={getAdditionalValueClassname(
                        'commitHash'
                      )}
                    />
                  </Field>
                );
              })()
            ) : (
              <Field label="Hash">
                <span className={getAdditionalValueClassname('commitHash')}>
                  {gitCodeVersion.data.commitHash}
                </span>
              </Field>
            ))}
          {gitCodeVersion.data.execPath &&
            (() => {
              if (gitCodeVersion.data.remoteRepoUrl) {
                const remoteRepoUrlInfo = parseGitRemoteRepoUrl(
                  gitCodeVersion.data.remoteRepoUrl
                );
                const url = `https://github.com/${remoteRepoUrlInfo.userName}/${
                  remoteRepoUrlInfo.repositoryInfo.name
                }/blob/${gitCodeVersion.data.commitHash}/${
                  gitCodeVersion.data.execPath
                }`;
                return (
                  <Field label="Exec path">
                    <ExternalLink
                      url={url}
                      text={gitCodeVersion.data.execPath}
                      additionalClassname={getAdditionalValueClassname(
                        'execPath'
                      )}
                    />
                  </Field>
                );
              }
              return (
                <Field label="Hash">
                  <span className={getAdditionalValueClassname('execPath')}>
                    {gitCodeVersion.data.execPath}
                  </span>
                </Field>
              );
            })()}
          {gitCodeVersion.data.isDirty !== undefined ? (
            <Field label="Dirty">
              <span className={getAdditionalValueClassname('isDirty')}>
                {String(gitCodeVersion.data.isDirty)}
              </span>
            </Field>
          ) : null}
          {gitCodeVersion.data.remoteRepoUrl &&
            (() => {
              const remoteRepoUrlInfo = parseGitRemoteRepoUrl(
                gitCodeVersion.data.remoteRepoUrl
              );
              const url = `https://github.com/${remoteRepoUrlInfo.userName}/${
                remoteRepoUrlInfo.repositoryInfo.nameWithExtension
              }`;
              const text = `${remoteRepoUrlInfo.userName}/${
                remoteRepoUrlInfo.repositoryInfo.nameWithExtension
              }`;
              return (
                <Field label="Repo">
                  <ExternalLink
                    url={url}
                    text={text}
                    additionalClassname={getAdditionalValueClassname(
                      'remoteRepoUrl'
                    )}
                  />
                </Field>
              );
            })()}
        </>
      )}
    </PilePopup.Fields>
  );
};

const GitCodeVersionButton = ({
  gitCodeVersion,
  additionalClassnameButton,
}: ILocalProps) => {
  return (
    <PileWithPopup
      pileComponent={({ showPopup }) => (
        <GitCodeVersionPile
          additionalClassname={additionalClassnameButton}
          onClick={showPopup}
        />
      )}
      popupComponent={({ isOpen, closePopup }) => (
        <GitCodeVersionPopup
          isOpen={isOpen}
          gitCodeVersion={gitCodeVersion}
          onClose={closePopup}
        />
      )}
    />
  );
};

export default GitCodeVersionButton;

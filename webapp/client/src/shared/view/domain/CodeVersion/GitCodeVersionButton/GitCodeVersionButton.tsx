import * as React from 'react';

import { IGitCodeVersion } from 'shared/models/CodeVersion';
import ExternalLink from 'shared/view/elements/ExternalLink/ExternalLink';

import * as Github from 'shared/utils/github/github';
import Pile from 'shared/view/elements/PileWithPopup/Pile/Pile';
import PilePopup from 'shared/view/elements/PileWithPopup/PilePopup/PilePopup';
import PileWithPopup from 'shared/view/elements/PileWithPopup/PileWithPopup/PileWithPopup';

const GitCodeVersionPopup = ({
  gitCodeVersion,
  isOpen,
  additionalFields,
  onClose,
}: {
  gitCodeVersion: IGitCodeVersion;
  isOpen: boolean;
  additionalFields?: Array<{ label: string; content: React.ReactNode }>;
  onClose(): void;
}) => {
  return isOpen ? (
    <PilePopup
      title={'Code Version'}
      titleIcon="codepen"
      contentLabel="git-code-version-action"
      isOpen={isOpen}
      onRequestClose={onClose}
    >
      <GitCodeVersionPopupFields
        gitCodeVersion={gitCodeVersion}
        additionalFields={additionalFields}
      />
    </PilePopup>
  ) : null;
};

export const GitCodeVersionPopupFields = ({
  gitCodeVersion,
  additionalFields,
  getAdditionalValueClassname = () => undefined,
}: {
  getAdditionalValueClassname?: (str: string) => string | undefined;
  gitCodeVersion: IGitCodeVersion;
  additionalFields?: Array<{ label: string; content: React.ReactNode }>;
}) => {
  const data = gitCodeVersion.data;
  return (
    <PilePopup.Fields>
      {Field => (
        <>
          {(additionalFields || []).map(field => (
            <Field label={field.label}>{field.content}</Field>
          ))}
          {data.commitHash && (
            <Field label="Hash">
              {data.remoteRepoUrl && data.remoteRepoUrl.type === 'github' ? (
                <ExternalLink
                  url={Github.makeCommitUrl(
                    data.remoteRepoUrl.value,
                    data.commitHash
                  )}
                  text={data.commitHash}
                  additionalClassname={getAdditionalValueClassname(
                    'commitHash'
                  )}
                />
              ) : (
                <span className={getAdditionalValueClassname('commitHash')}>
                  {data.commitHash}
                </span>
              )}
            </Field>
          )}
          {data.execPath && (
            <Field label="Exec path">
              {data.remoteRepoUrl &&
              data.commitHash &&
              data.remoteRepoUrl.type === 'github' ? (
                <ExternalLink
                  url={Github.makeRepoBlobUrl(data.remoteRepoUrl.value, {
                    commitHash: data.commitHash,
                    execPath: data.execPath,
                  })}
                  text={data.execPath}
                  additionalClassname={getAdditionalValueClassname('execPath')}
                />
              ) : (
                <span className={getAdditionalValueClassname('execPath')}>
                  {data.execPath}
                </span>
              )}
            </Field>
          )}
          {data.isDirty !== undefined ? (
            <Field label="Dirty">
              <span className={getAdditionalValueClassname('isDirty')}>
                {String(data.isDirty)}
              </span>
            </Field>
          ) : null}
          {data.remoteRepoUrl && (
            <Field label="Repo">
              {(() => {
                if (data.remoteRepoUrl.type === 'github') {
                  const {
                    url,
                    shortName,
                  } = Github.makeRepoUrlWithRepoShortName(
                    data.remoteRepoUrl.value
                  );
                  return (
                    <ExternalLink
                      url={url}
                      text={shortName}
                      additionalClassname={getAdditionalValueClassname(
                        'remoteRepoUrl'
                      )}
                    />
                  );
                } else {
                  return (
                    <span
                      className={getAdditionalValueClassname('remoteRepoUrl')}
                    >
                      {data.remoteRepoUrl.value}
                    </span>
                  );
                }
              })()}
            </Field>
          )}
        </>
      )}
    </PilePopup.Fields>
  );
};

export const GitCodeVersionPile = ({
  additionalClassname,
  onClick,
  label,
}: {
  additionalClassname: string | undefined;
  label?: string;
  onClick: () => void;
}) => (
  <Pile
    title="view git code version"
    dataTest="git-code-version"
    label={label || 'Git Code Version'}
    iconType="codepen"
    additionalClassname={additionalClassname}
    onClick={onClick}
  />
);

interface ILocalProps {
  gitCodeVersion: IGitCodeVersion;
  additionalClassnameButton?: string;
  popupProps?: {
    additionalFields?: Array<{ label: string; content: React.ReactNode }>;
  };
  pileProps?: {
    label: string;
  };
}

const GitCodeVersionButton = ({
  gitCodeVersion,
  additionalClassnameButton,
  popupProps,
  pileProps,
}: ILocalProps) => {
  return (
    <PileWithPopup
      pileComponent={({ showPopup }) => (
        <GitCodeVersionPile
          additionalClassname={additionalClassnameButton}
          label={pileProps ? pileProps.label : undefined}
          onClick={showPopup}
        />
      )}
      popupComponent={({ isOpen, closePopup }) => (
        <GitCodeVersionPopup
          isOpen={isOpen}
          gitCodeVersion={gitCodeVersion}
          additionalFields={
            popupProps ? popupProps.additionalFields : undefined
          }
          onClose={closePopup}
        />
      )}
    />
  );
};

export default GitCodeVersionButton;

import cn from 'classnames';
import * as React from 'react';
import { Link } from 'react-router-dom';
import * as R from 'ramda';
import moment from 'moment';

import {
  IResult,
  EntityResultCommonData,
} from 'core/shared/models/HighLevelSearch';
import { exhaustiveCheck } from 'core/shared/utils/exhaustiveCheck';
import findHighlight from 'core/shared/utils/findHighlight';
import { Icon, IconType } from 'core/shared/view/elements/Icon/Icon';
import routes from 'routes';
import { IWorkspace } from 'core/shared/models/Workspace';
import {
  makeDefaultExprNameFilter,
  makeURLFilters,
} from 'core/features/filter/Model';
import { NA } from 'core/shared/view/elements/PageComponents';

import styles from './Result.module.css';
import { unknownUser } from 'core/shared/models/User';

interface ILocalProps {
  searchValue: string;
  result: IResult;
  workspaceName: IWorkspace['name'];
}

const Result = ({ result, searchValue, workspaceName }: ILocalProps) => {
  switch (result.entityType) {
    case 'project':
      return (
        <EntityResult
          data={result}
          searchValue={searchValue}
          icon="folder"
          pathname={routes.projectSummary.getRedirectPath({
            workspaceName,
            projectId: result.id,
          })}
        />
      );
    case 'experiment':
      return (
        <EntityResult
          data={result}
          pathname={routes.experimentRuns.getRedirectPathWithQueryParams({
            params: {
              workspaceName,
              projectId: result.project.id,
            },
            queryParams: {
              filters: makeURLFilters([makeDefaultExprNameFilter(result.name)]),
            },
          })}
          additionalLabels={[
            {
              label: 'Project',
              value: result.project.name,
            },
          ]}
          searchValue={searchValue}
          icon="experiment"
        />
      );
    case 'experimentRun':
      return (
        <EntityResult
          data={result}
          pathname={routes.modelRecord.getRedirectPath({
            workspaceName,
            projectId: result.project.id,
            modelRecordId: result.id,
          })}
          additionalLabels={[
            {
              label: 'Project',
              value: result.project.name,
            },
            {
              label: 'Experiment',
              value: result.experiment.name,
            },
          ]}
          searchValue={searchValue}
          icon="experiment-run"
        />
      );
    case 'dataset':
      return (
        <EntityResult
          data={result}
          pathname={routes.datasetSummary.getRedirectPath({
            workspaceName,
            datasetId: result.id,
          })}
          searchValue={searchValue}
          icon="datasets"
        />
      );
    case 'repository':
      return (
        <EntityResult
          tagsLabel="Labels"
          data={{
            ...result,
            tags: result.labels,
          }}
          pathname={routes.repositoryData.getRedirectPath({
            workspaceName,
            repositoryName: result.name,
          })}
          searchValue={searchValue}
          icon="repository"
        />
      );
    default:
      return exhaustiveCheck(result, '');
  }
};

const EntityResult = ({
  tagsLabel,
  searchValue,
  icon,
  data,
  pathname,
  additionalLabels,
}: {
  tagsLabel?: string;
  data: EntityResultCommonData;
  icon: IconType;
  pathname: string;
  searchValue: string;
  additionalLabels?: Array<{ label: string; value: string }>;
}) => {
  return (
    <Link className={styles.result} to={pathname}>
      <div className={styles.header}>
        <Icon type={icon} className={styles.icon} />
        <NameWithHighlithedMatches name={data.name} searchValue={searchValue} />
      </div>
      <div className={cn(styles.detailsSection, { [styles.additional]: true })}>
        {(additionalLabels || []).map(({ label, value }, i) => (
          <LabeledDetail label={label} value={value} key={i} />
        ))}
      </div>
      <div className={styles.detailsSection}>
        <LabeledDetail
          label={tagsLabel || 'Tags'}
          value={
            <TagsWithHighlithedMatches
              searchValue={searchValue}
              tags={data.tags}
            />
          }
        />
        <LabeledDetail label="Created By" value={unknownUser.username} />
      </div>
      <div className={cn(styles.detailsSection, { [styles.dates]: true })}>
        <LabeledDetail
          label="Created"
          value={moment(data.dateCreated).format('MM/DD/YYYY HH:mm:ss')}
        />
        <LabeledDetail
          label="Modified"
          value={moment(data.dateUpdated).format('MM/DD/YYYY HH:mm:ss')}
        />
      </div>
    </Link>
  );
};

const NameWithHighlithedMatches = ({
  searchValue,
  name,
}: {
  searchValue: string;
  name: string;
}) => {
  const parts = findHighlight({
    searchWord: searchValue,
    textToHighlight: name,
    settings: { allMatches: true, caseIntensive: true },
  });

  return (
    <span>
      {parts.map((part, i) => (
        <span
          className={cn(styles.name, { [styles.match]: part.isMatch })}
          key={i}
        >
          {part.text}
        </span>
      ))}
    </span>
  );
};

const TagsWithHighlithedMatches = ({
  searchValue,
  tags,
}: {
  searchValue: string;
  tags: string[];
}) => {
  if (tags.length === 0) {
    return <span className={styles.tags}>{NA}</span>;
  }
  const sortedTags = R.sort(
    tag => (new RegExp(searchValue, 'gi').test(tag) ? -1 : 0),
    tags
  );

  const TagWithHighlightedMatches = ({ tag }: { tag: string }) => {
    const parts = findHighlight({
      searchWord: searchValue,
      textToHighlight: tag,
      settings: { allMatches: true, caseIntensive: true },
    });

    return (
      <span>
        {parts.map((part, i) => (
          <span className={cn({ [styles.match]: part.isMatch })} key={i}>
            {part.text}
          </span>
        ))}
      </span>
    );
  };

  return (
    <span className={styles.tags}>
      {sortedTags.map((tag, i) => (
        <>
          <TagWithHighlightedMatches tag={tag} />
          {i < sortedTags.length - 1 ? ', ' : ''}
        </>
      ))}
    </span>
  );
};

const LabeledDetail = ({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) => {
  return (
    <div className={styles.detail}>
      <span className={styles.detail__label}>{label}:&nbsp;</span>
      <span className={styles.detail__value}>{value}</span>
    </div>
  );
};

export default Result;

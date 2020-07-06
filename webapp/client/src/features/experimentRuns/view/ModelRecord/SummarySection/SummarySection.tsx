import * as React from 'react';
import { Link } from 'react-router-dom';
import { useSelector } from 'react-redux';

import ModelRecord from 'shared/models/ModelRecord';
import ProjectEntityDescriptionManager from 'features/descriptionManager/view/ProjectEntityDescriptionManager/ProjectEntityDescriptionManager';
import ProjectEntityTagsManager from 'features/tagsManager/view/ProjectEntityTagsManager/ProjectEntityTagsManager';
import { ShowCommentsButtonWithAuthor } from 'features/comments';
import { getFormattedDateTime } from 'shared/utils/formatters/dateTime';
import { Project } from 'shared/models/Project';
import { TextWithCopyTooltip } from 'shared/view/elements/TextWithCopyTooltip/TextWithCopyTooltip';
import routes from 'shared/routes';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';
import {
  makeURLFilters,
  makeDefaultExprNameFilter,
} from 'shared/models/Filters';

import Section from '../shared/Section/Section';
import styles from './SummarySection.module.css';
import { RecordInfo } from '../shared/RecordInfo/RecordInfo';

interface ILocalProps {
  modelRecord: ModelRecord;
  project: Project;
}

const SummarySection = ({ modelRecord, project }: ILocalProps) => {
  const workspaceName = useSelector(selectCurrentWorkspaceName);

  return (
    <Section title="Summary">
      <div className={styles.content}>
        <div className={styles.column}>
          <RecordInfo
            label="Owner"
            valueTitle={modelRecord.owner.username}
            withValueTruncation={true}
          >
            {modelRecord.owner.username}
          </RecordInfo>
          <RecordInfo label="Description" withValueTruncation={false}>
            <ProjectEntityDescriptionManager
              entityType="experimentRun"
              entityId={modelRecord.id}
              description={modelRecord.description}
            />
          </RecordInfo>
          <RecordInfo label="Tags" withValueTruncation={false}>
            <ProjectEntityTagsManager
              id={modelRecord.id}
              projectId={modelRecord.projectId}
              tags={modelRecord.tags}
              isDraggableTags={false}
              entityType="experimentRun"
            />
          </RecordInfo>
          <RecordInfo
            label="Comments"
            labelAlign="center"
            withValueTruncation={false}
          >
            <ShowCommentsButtonWithAuthor
              entityInfo={{ id: modelRecord.id, name: modelRecord.name }}
              buttonType="badge"
            />
          </RecordInfo>
        </div>
        <div className={styles.column}>
          <RecordInfo label="Run ID">
            <TextWithCopyTooltip copyText={modelRecord.id} withEllipsis={true}>
              {modelRecord.id}
            </TextWithCopyTooltip>
          </RecordInfo>
          <RecordInfo label="Experiment">
            <Link
              className={styles.link}
              to={routes.experimentRuns.getRedirectPathWithQueryParams({
                params: {
                  workspaceName,
                  projectId: project.id,
                },
                queryParams: {
                  filters: makeURLFilters([
                    makeDefaultExprNameFilter(modelRecord.shortExperiment.name),
                  ]),
                },
              })}
            >
              {modelRecord.shortExperiment.name}
            </Link>
          </RecordInfo>
          <RecordInfo label="Project">
            <Link
              className={styles.link}
              to={routes.projectSummary.getRedirectPath({
                workspaceName,
                projectId: project.id,
              })}
            >
              {project.name}
            </Link>
          </RecordInfo>
          <RecordInfo label="Timestamp">
            {getFormattedDateTime(modelRecord.dateCreated)}
          </RecordInfo>
        </div>
      </div>
    </Section>
  );
};

export default SummarySection;

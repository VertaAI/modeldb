import * as React from 'react';
import { useSelector } from 'react-redux';
import * as R from 'ramda';

import Observations from 'shared/view/domain/ModelRecord/ModelRecordProps/Observations/Observations/Observations';
import Parameters from 'shared/view/domain/ModelRecord/ModelRecordProps/Parameters/Parameters';
import ClientSuggestion from 'shared/view/domain/ModelRecord/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import vertaDocLinks from 'shared/utils/globalConstants/vertaDocLinks';
import withProps from 'shared/utils/react/withProps';
import IdView from 'shared/view/elements/IdView/IdView';
import TagBlock from 'shared/view/elements/TagBlock/TagBlock';
import { selectProject } from 'features/projects/store';
import {
  ComparedMultipleModels,
  IModelsDifferentProps,
  minNumberOfModelsForComparing,
  getComparedMultipleModels,
  compareModels,
} from '../../../store/compareModels/compareModels';
import useRequest from 'shared/view/hooks/useRequest';
import Placeholder from 'shared/view/elements/Placeholder/Placeholder';
import { IApplicationState } from 'setup/store/store';
import { ExperimentRunsDataService } from 'services/experimentRuns';
import DefaultMatchRemoteData from 'shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';

import {
  getDiffValueBgClassname,
  getDiffValueBorderClassname,
} from '../shared/DiffHighlight/DiffHighlight';
import ComparableDatasets from './ComparableDatasets/ComparableDatasets';
import styles from './CompareModels.module.css';
import CompareModelsTableView, {
  PropDefinition,
  IPropDefinitionRenderProps,
} from './CompareModelsTable/CompareModelsTable';
import ComparableAttributes from './ComparableAttributes/ComparableAttributes';
import ComparableCodeVersion from './ComparableCodeVersion/ComparableCodeVersion';
import { NA } from 'shared/view/elements/PageComponents';
import Artifacts from 'features/artifactManager/view/Artifacts/Artifacts';
import { CompareObservartionsChart } from 'features/compareModels/view/CompareEntities/CompareModels/CompareObservationsChart/CompareObservationsChart';

interface ILocalProps {
  comparedModelIds: string[];
  projectId: string;
}

const CompareMultipleModels = ({
  comparedModelIds,
  projectId,
}: ILocalProps) => {
  const project = useSelector((state: IApplicationState) =>
    selectProject(state, projectId)
  );
  const { data: models, communication } = useRequest(async () =>
    new ExperimentRunsDataService().loadExperimentRunsByIds(
      projectId,
      comparedModelIds
    )
  );

  return (
    <DefaultMatchRemoteData data={models} communication={communication}>
      {loadedModels => {
        const comparedModels = getComparedMultipleModels(loadedModels);
        return comparedModels ? (
          <CompareModelsTable
            comparedModels={comparedModels}
            modelsDifferentProps={compareModels(comparedModels)}
            projectName={project?.name || ''}
          />
        ) : (
          <Placeholder>
            You should select at least {minNumberOfModelsForComparing} for
            comparing
          </Placeholder>
        );
      }}
    </DefaultMatchRemoteData>
  );
};

const CompareModelsTable = ({
  projectName,
  comparedModels,
  modelsDifferentProps,
}: {
  projectName: string;
  comparedModels: ComparedMultipleModels;
  modelsDifferentProps: IModelsDifferentProps;
}) => {
  const columns = R.fromPairs(
    comparedModels.map((_, i) => [String(i + 1), { title: `Model ${i + 1}` }])
  );
  return (
    <div className={styles.content}>
      <div className={'compare_table'}>
        <CompareModelsTableView
          models={comparedModels}
          modelsDifferentProps={modelsDifferentProps}
          columns={columns}
        >
          <PropDefinition
            prop="id"
            title="ID"
            getValue={modelRecord => modelRecord.id}
            render={withProps(SingleValue)({ propertyType: 'id' })}
          />
          <PropDefinition
            prop="ownerId"
            title="Owner"
            getValue={modelRecord => modelRecord.owner.username}
            render={withProps(SingleValue)({ propertyType: 'ownerId' })}
          />
          <PropDefinition
            prop="experimentId"
            title="Experiment"
            getValue={modelRecord => modelRecord.shortExperiment.name}
            render={withProps(SingleValue)({
              propertyType: 'experimentId',
            })}
          />
          <PropDefinition
            prop="projectId"
            title="Project"
            getValue={_ => projectName}
            render={withProps(SingleValue)({
              propertyType: 'projectId',
            })}
          />
          <PropDefinition
            prop="tags"
            title="Tags"
            getValue={model => model.tags}
            render={({ currentEntityInfo: { value } }) =>
              value.length !== 0 ? (
                <TagBlock tags={value} />
              ) : (
                <ClientSuggestion
                  fieldName={'tags'}
                  clientMethod={'log_tags()'}
                  link={vertaDocLinks.log_tags}
                />
              )
            }
          />
          <PropDefinition
            prop="hyperparameters"
            title="Hyperparameters"
            getValue={model => model.hyperparameters}
            render={({
              currentEntityInfo: { value, entityType: modelType, diffInfo },
            }) => (
              <Parameters
                prop="hyperparameters"
                parameters={value}
                getValueClassname={key =>
                  getDiffValueBgClassname(modelType, diffInfo[key])
                }
              />
            )}
          />
          <PropDefinition
            prop="metrics"
            title="Metrics"
            getValue={model => model.metrics}
            render={({
              currentEntityInfo: { value, entityType: modelType, diffInfo },
            }) => (
              <Parameters
                prop="metrics"
                parameters={value}
                getValueClassname={key =>
                  getDiffValueBgClassname(modelType, diffInfo[key])
                }
              />
            )}
          />
          <PropDefinition
            prop="attributes"
            title="Attributes"
            getValue={model => model.attributes}
            render={({ currentEntityInfo, allEntitiesInfo }) => {
              return (
                <ComparableAttributes
                  currentAttributesInfo={currentEntityInfo}
                  allModelsAttributesInfo={allEntitiesInfo}
                  docLink={vertaDocLinks.log_attribute}
                />
              );
            }}
          />
          <PropDefinition
            prop="artifacts"
            title="Artifacts"
            getValue={model => model.artifacts}
            render={({ currentEntityInfo, allEntitiesInfo }) => {
              return (
                <Artifacts
                  entityType="experimentRun"
                  entitiyId={currentEntityInfo.model.id}
                  artifacts={currentEntityInfo.value}
                  docLink={vertaDocLinks.log_artifact}
                  getArtifactClassname={key =>
                    getDiffValueBorderClassname(
                      currentEntityInfo.entityType,
                      currentEntityInfo.diffInfo[key]
                    )
                  }
                />
              );
            }}
          />
          <PropDefinition
            prop="datasets"
            title="Datasets"
            getValue={model => model.datasets}
            render={({ currentEntityInfo, allEntitiesInfo }) => (
              <ComparableDatasets
                allModelsDatasetsInfo={allEntitiesInfo}
                currentDatasetsInfo={currentEntityInfo}
                docLink={vertaDocLinks.log_dataset}
              />
            )}
          />
          <PropDefinition
            prop="observations"
            title="Observations"
            getValue={model => model.observations}
            render={({ currentEntityInfo: { value } }) => (
              <Observations
                observations={value}
                docLink={vertaDocLinks.log_observations}
              />
            )}
          />
          <PropDefinition
            prop="codeVersion"
            title="Code Versions"
            getValue={model => model.codeVersion}
            render={({ currentEntityInfo, allEntitiesInfo }) =>
              currentEntityInfo.value ? (
                <ComparableCodeVersion
                  currentEntityInfo={{
                    ...currentEntityInfo,
                    value: currentEntityInfo.value,
                  }}
                  allEntitiesInfo={allEntitiesInfo}
                />
              ) : (
                NA
              )
            }
          />
        </CompareModelsTableView>
        <div className={styles.compareObservartionsChart}>
          <CompareObservartionsChart modelsObservations={comparedModels} />
        </div>
      </div>
    </div>
  );
};

type SingleValuePropertyType = 'id' | 'experimentId' | 'projectId' | 'ownerId';
function SingleValue<
  Props extends IPropDefinitionRenderProps<SingleValuePropertyType>
>({
  currentEntityInfo: { value, entityType: modelType, diffInfo },
  propertyType,
}: Props & { propertyType: SingleValuePropertyType }) {
  return (
    <span
      className={getDiffValueBgClassname(modelType, diffInfo)}
      data-test={`property-value-${propertyType}`}
    >
      {propertyType === 'id' ? <IdView value={value} /> : value}
    </span>
  );
}

export type ICompareModelsLocalProps = ILocalProps;
export default CompareMultipleModels;

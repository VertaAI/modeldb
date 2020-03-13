import * as React from 'react';
import { connect } from 'react-redux';

import Artifacts from 'components/ModelRecordProps/Artifacts/Artifacts/Artifacts';
import Observations from 'components/ModelRecordProps/Observations/Observations/Observations';
import Parameters from 'components/ModelRecordProps/Parameters/Parameters';
import ClientSuggestion from 'components/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import vertaDocLinks from 'core/shared/utils/globalConstants/vertaDocLinks';
import withProps from 'core/shared/utils/react/withProps';
import IdView from 'core/shared/view/elements/IdView/IdView';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import TagBlock from 'core/shared/view/elements/TagBlock/TagBlock';
import ModelRecord from 'models/ModelRecord';
import { Project } from 'models/Project';
import {
  ComparedEntityIds,
  IModelsDifferentProps,
  selectModelsDifferentProps,
  ComparedModels,
  selectComparedModels,
  EntityType,
} from 'store/compareEntities';
import {
  loadExperimentRun,
  selectIsLoadingExperimentRun,
} from 'store/experimentRuns';
import { selectProject } from 'store/projects';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import ComparableAttributes from '../shared/ComparableAttributes/ComparableAttributes';
import {
  getDiffValueBgClassname,
  getDiffValueBorderClassname,
} from '../shared/DiffHighlight/DiffHighlight';
import ComparableCodeVersion from './ComparableCodeVersion/ComparableCodeVersion';
import ComparableDatasets from './ComparableDatasets/ComparableDatasets';
import styles from './CompareModels.module.css';
import CompareModelsTable, {
  PropDefinition,
  IPropDefinitionRenderProps,
} from './CompareModelsTable/CompareModelsTable';

interface ILocalProps {
  comparedModelIds: Required<ComparedEntityIds>;
  projectId: string;
}

interface IPropsFromState {
  modelsDifferentProps: IModelsDifferentProps | undefined;
  comparedModels: ComparedModels;
  isLoadingComparedModels: boolean;
  project: Project | undefined;
}

type AllProps = ILocalProps & IPropsFromState & IConnectedReduxProps;

class CompareModels extends React.PureComponent<AllProps> {
  public componentDidMount() {
    const { dispatch, projectId, comparedModelIds } = this.props;
    dispatch(loadExperimentRun(projectId, comparedModelIds[0]));
    dispatch(loadExperimentRun(projectId, comparedModelIds[1]));
  }

  public render() {
    const {
      modelsDifferentProps,
      comparedModels,
      project,
      isLoadingComparedModels,
    } = this.props;

    return (
      <div className={styles.root}>
        {isLoadingComparedModels ||
        comparedModels.length !== 2 ||
        !modelsDifferentProps ? (
          <Preloader variant="dots" />
        ) : (
          <div className={styles.content}>
            <div className={'compare_table'}>
              <CompareModelsTable
                models={comparedModels as [ModelRecord, ModelRecord]}
                modelsDifferentProps={modelsDifferentProps}
                columns={{
                  property: { title: 'Properties', width: 180 },
                  [EntityType.entity1]: { title: 'Model 1' },
                  [EntityType.entity2]: { title: 'Model 2' },
                }}
              >
                <PropDefinition
                  prop="id"
                  title="ID"
                  getValue={modelRecord => modelRecord.id}
                  render={withProps(SingleValue)({ propertyType: 'id' })}
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
                  getValue={_ => (project ? project.name : '')}
                  render={withProps(SingleValue)({ propertyType: 'projectId' })}
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
                    currentEntityInfo: { value, entityType: modelType },
                    diffInfo: differentValues,
                  }) => (
                    <Parameters
                      prop="hyperparameters"
                      parameters={value}
                      getValueClassname={key =>
                        getDiffValueBgClassname(modelType, differentValues[key])
                      }
                    />
                  )}
                />
                <PropDefinition
                  prop="metrics"
                  title="Metrics"
                  getValue={model => model.metrics}
                  render={({
                    currentEntityInfo: { value, entityType: modelType },
                    diffInfo: differentValues,
                  }) => (
                    <Parameters
                      prop="metrics"
                      parameters={value}
                      getValueClassname={key =>
                        getDiffValueBgClassname(modelType, differentValues[key])
                      }
                    />
                  )}
                />
                <PropDefinition
                  prop="attributes"
                  title="Attributes"
                  getValue={model => model.attributes}
                  render={({
                    currentEntityInfo: { value, entityType: modelType },
                    diffInfo,
                  }) => {
                    const [model1, model2] = comparedModels as [
                      ModelRecord,
                      ModelRecord
                    ];
                    return (
                      <ComparableAttributes
                        entityType={modelType}
                        entity1Attributes={model1.attributes}
                        entity2Attributes={model2.attributes}
                        diffInfo={diffInfo}
                        docLink={vertaDocLinks.log_attribute}
                      />
                    );
                  }}
                />
                <PropDefinition
                  prop="artifacts"
                  title="Artifacts"
                  getValue={model => model.artifacts}
                  render={({
                    currentEntityInfo: { value, entityType: modelType, model },
                    diffInfo: differentValues,
                  }) => (
                    <Artifacts
                      entityType="experimentRun"
                      entitiyId={model.id}
                      artifacts={value}
                      docLink={vertaDocLinks.log_artifact}
                      getArtifactClassname={key =>
                        getDiffValueBorderClassname(
                          modelType,
                          differentValues[key]
                        )
                      }
                    />
                  )}
                />
                <PropDefinition
                  prop="datasets"
                  title="Datasets"
                  getValue={model => model.datasets}
                  render={({
                    currentEntityInfo: { entityType: modelType },
                    diffInfo: differentValues,
                  }) => (
                    <ComparableDatasets
                      entity1Datasets={comparedModels[0]!.datasets}
                      entity2Datasets={comparedModels[1]!.datasets}
                      diffInfo={differentValues}
                      entityType={modelType}
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
                  title="Code Version"
                  getValue={model => model.codeVersion}
                  render={({ currentEntityInfo, otherEntityInfo, diffInfo }) =>
                    currentEntityInfo.value ? (
                      <ComparableCodeVersion
                        currentEntityInfo={{
                          codeVersion: currentEntityInfo.value,
                          entityType: currentEntityInfo.entityType,
                          id: currentEntityInfo.model.id,
                        }}
                        otherEntityInfo={{
                          codeVersion: otherEntityInfo.value,
                          entityType: otherEntityInfo.entityType,
                          id: otherEntityInfo.model.id,
                        }}
                        diffInfo={diffInfo}
                      />
                    ) : (
                      '-'
                    )
                  }
                />
              </CompareModelsTable>
            </div>
          </div>
        )}
      </div>
    );
  }
}

type SingleValuePropertyType = 'id' | 'experimentId' | 'projectId';
function SingleValue<
  Props extends IPropDefinitionRenderProps<SingleValuePropertyType>
>({
  currentEntityInfo: { value, model, entityType: modelType },
  diffInfo: isDifferent,
  propertyType,
}: Props & { propertyType: SingleValuePropertyType }) {
  return (
    <span
      className={getDiffValueBgClassname(modelType, isDifferent)}
      data-test={`property-value-${propertyType}`}
    >
      {propertyType === 'id' ? <IdView value={value} /> : value}
    </span>
  );
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    modelsDifferentProps: selectModelsDifferentProps(
      state,
      localProps.projectId,
      localProps.comparedModelIds
    ),
    comparedModels: selectComparedModels(
      state,
      localProps.projectId,
      localProps.comparedModelIds
    ),
    project: selectProject(state, localProps.projectId),
    isLoadingComparedModels:
      selectIsLoadingExperimentRun(state, localProps.comparedModelIds[0]) ||
      selectIsLoadingExperimentRun(state, localProps.comparedModelIds[1]),
  };
};

export type ICompareModelsLocalProps = ILocalProps;
export default connect(mapStateToProps)(CompareModels);

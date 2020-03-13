import * as R from 'ramda';

import { Artifact } from 'core/shared/models/Artifact';
import { ICodeVersion } from 'core/shared/models/CodeVersion';
import {
  getKeyValuePairsDiff,
  getObjsPropsDiffByPred,
  getObjsPropsDiff,
} from 'core/shared/utils/collection';
import {
  IDatasetVersion,
  IRawDatasetVersion,
  IQueryDatasetVersion,
  IPathBasedDatasetVersion,
  IDatasetPathPartInfo,
} from 'models/DatasetVersion';
import ModelRecord from 'models/ModelRecord';
import { selectDatasetVersion } from 'store/datasetVersions';
import { selectExperimentRun } from 'store/experimentRuns';

import { IAttribute } from 'core/shared/models/Attribute';
import { IApplicationState } from '../store';
import {
  ICompareEntitiesState,
  ComparedEntityIds,
  ComparedModels,
  IModelsDifferentProps,
  ComparedArtifactPropType,
  ComparedDatasetVersions,
  IDatasetVersionsDifferentProps,
  IDatasetVersionCommonDifferentProps,
  IRawDatasetVersionDifferentProps,
  IQueryDatasetVersionDifferentProps,
  IPathDatasetVersionDifferentProps,
  IDiffDatasetPathInfos,
} from './types';

const selectState = (state: IApplicationState): ICompareEntitiesState =>
  state.compareEntities;

export const selectComparedEntityIds = (
  state: IApplicationState,
  projectId: string
) => selectState(state).data.comparedEntityIdsByContainerId[projectId] || [];

export const selectIsEnableEntitiesComparing = (
  state: IApplicationState,
  projectId: string
) => selectComparedEntityIds(state, projectId).length === 2;

export const selectIsDisabledSelectionEntitiesForComparing = (
  state: IApplicationState,
  projectId: string
) => selectIsEnableEntitiesComparing(state, projectId);

export const selectIsComparedEntity = (
  state: IApplicationState,
  projectId: string,
  modelId: string
) => selectComparedEntityIds(state, projectId).includes(modelId);

// models

export const selectComparedModels = (
  state: IApplicationState,
  projectId: string,
  modelIds: Required<ComparedEntityIds>
): ComparedModels =>
  modelIds
    .map(id => selectExperimentRun(state, id!))
    .filter(Boolean) as ComparedModels;

export const selectModelsDifferentProps = (
  state: IApplicationState,
  projectId: string,
  modelIds: Required<ComparedEntityIds>
) => {
  const comparedModels: ComparedModels = selectComparedModels(
    state,
    projectId,
    modelIds
  );
  return comparedModels.length === 2
    ? getModelsDifferentProps(...(comparedModels as Required<ComparedModels>))
    : undefined;
};
const getModelsDifferentProps = (
  modelRecord1: ModelRecord,
  modelRecord2: ModelRecord
): IModelsDifferentProps => {
  return {
    codeVersion: compareCodeVersion(
      modelRecord1.codeVersion,
      modelRecord2.codeVersion
    ),
    hyperparameters: getKeyValuePairsDiff(
      modelRecord1.hyperparameters,
      modelRecord2.hyperparameters
    ),
    metrics: getKeyValuePairsDiff(modelRecord1.metrics, modelRecord2.metrics),
    attributes: compareAttributes(
      modelRecord1.attributes,
      modelRecord2.attributes
    ),
    id: modelRecord1.id !== modelRecord2.id,
    experimentId: modelRecord1.experimentId !== modelRecord2.experimentId,
    projectId: modelRecord1.projectId !== modelRecord2.projectId,
    artifacts: compareArtifacts(modelRecord1.artifacts, modelRecord2.artifacts),
    tags: !R.equals(modelRecord1.tags, modelRecord2.tags),
    datasets: compareArtifacts(modelRecord1.datasets, modelRecord2.datasets),
  };
};

const compareAttributes = (
  attributes1: IAttribute[],
  attributes2: IAttribute[]
): IModelsDifferentProps['attributes'] => {
  const attributesKeys = R.uniq(
    attributes1.map(({ key }) => key).concat(attributes2.map(({ key }) => key))
  );
  return R.fromPairs(
    attributesKeys.map(attributeKey => {
      const attribute1 = attributes1.find(({ key }) => key === attributeKey);
      const attribute2 = attributes2.find(({ key }) => key === attributeKey);
      if ((attribute1 && !attribute2) || (!attribute1 && attribute2)) {
        return [attributeKey, { type: 'singleAttribute' }];
      }
      if (
        Array.isArray(attribute1!.value) &&
        Array.isArray(attribute2!.value)
      ) {
        return [
          attributeKey,
          {
            type: 'listValueTypes',
            diffInfo: getKeyValuePairsDiff(
              attribute1!.value.map(x => ({
                key: x,
                value: x,
              })),
              attribute2!.value.map(x => ({
                key: x,
                value: x,
              }))
            ),
          },
        ];
      }
      if ([attribute1!.value, attribute2!.value].some(Array.isArray)) {
        return [attributeKey, { type: 'differentValueTypes' }];
      }
      return [
        attributeKey,
        {
          type: 'singleValueTypes',
          isDifferent: attribute1!.value !== attribute2!.value,
        },
      ];
    })
  ) as any;
};

const compareCodeVersion = (
  codeVersion1?: ICodeVersion,
  codeVersion2?: ICodeVersion
): IModelsDifferentProps['codeVersion'] => {
  if (!codeVersion1 || !codeVersion2) {
    return { type: 'diffType' };
  }
  if (codeVersion1.type === 'artifact' && codeVersion2.type === 'artifact') {
    return {
      type: 'artifactCodeVersion',
      diffInfoByKeys: getObjsPropsDiff(
        R.pick(['key', 'path', 'type'], codeVersion1.data),
        R.pick(['key', 'path', 'type'], codeVersion2.data)
      ),
    };
  }
  if (codeVersion1.type === 'git' && codeVersion2.type === 'git') {
    return {
      type: 'gitCodeVersion',
      diffInfoByKeys: getObjsPropsDiff(codeVersion1.data, codeVersion2.data),
    };
  }
  return { type: 'diffType' };
};
const compareArtifacts = (artifacts1: Artifact[], artifacts2: Artifact[]) => {
  const artifactsToObjByKey = (artifacts: Artifact[]) =>
    R.fromPairs(artifacts.map(artifact => [artifact.key, artifact]));

  return getObjsPropsDiffByPred<Record<string, Artifact | undefined>>(
    (_, artifact1, artifact2) =>
      artifact1 && artifact2 ? isDiffArtifacts(artifact1, artifact2) : true,
    artifactsToObjByKey(artifacts1),
    artifactsToObjByKey(artifacts2)
  );
};
const isDiffArtifacts = (artifact1: Artifact, artifact2: Artifact): boolean => {
  const comparedArtifactProps = R.values(ComparedArtifactPropType);
  return comparedArtifactProps.some(
    prop => !R.equals(artifact1[prop], artifact2[prop])
  );
};

// dataset versions

export const selectComparedDatasetVersions = (
  state: IApplicationState,
  datasetVersionIds: Required<ComparedEntityIds>
): ComparedDatasetVersions => {
  return datasetVersionIds
    .map(id => selectDatasetVersion(state, id!))
    .filter(Boolean) as ComparedDatasetVersions;
};

export const selectDatasetVersionsDifferentProps = (
  state: IApplicationState,
  datasetVersionIds: Required<ComparedEntityIds>
) => {
  const comparedDatasetVersions: ComparedDatasetVersions = selectComparedDatasetVersions(
    state,
    datasetVersionIds
  );
  return comparedDatasetVersions.length === 2
    ? getDatasetVersionsDifferentProps(
        ...(comparedDatasetVersions as Required<ComparedDatasetVersions>)
      )
    : undefined;
};
const getDatasetVersionsDifferentProps = (
  datasetVersion1: IDatasetVersion,
  datasetVersion2: IDatasetVersion
): IDatasetVersionsDifferentProps => {
  if (datasetVersion1.type === 'raw' && datasetVersion2.type === 'raw') {
    return getRawDatasetVersionsDifferentProps(
      datasetVersion1,
      datasetVersion2
    );
  }
  if (datasetVersion1.type === 'query' && datasetVersion2.type === 'query') {
    return getQueryDatasetVersionsDifferentProps(
      datasetVersion1,
      datasetVersion2
    );
  }
  if (datasetVersion1.type === 'path' && datasetVersion2.type === 'path') {
    return getPathDatasetVersionsDifferentProps(
      datasetVersion1,
      datasetVersion2
    );
  }
  return {} as any;
};
const getDatasetVersionCommonDifferentProps = (
  datasetVersion1: IDatasetVersion,
  datasetVersion2: IDatasetVersion
): IDatasetVersionCommonDifferentProps => {
  return {
    id: datasetVersion1.id !== datasetVersion2.id,
    parentId: datasetVersion1.parentId !== datasetVersion2.parentId,
    tags: !R.equals(datasetVersion1.tags, datasetVersion2.tags),
    version: datasetVersion1.version !== datasetVersion2.version,
    attributes: compareAttributes(
      datasetVersion1.attributes,
      datasetVersion2.attributes
    ),
    dateLogged: datasetVersion1.dateLogged !== datasetVersion2.dateLogged,
  };
};
const getRawDatasetVersionsDifferentProps = (
  datasetVersion1: IRawDatasetVersion,
  datasetVersion2: IRawDatasetVersion
): IRawDatasetVersionDifferentProps => {
  return {
    ...getDatasetVersionCommonDifferentProps(datasetVersion1, datasetVersion2),
    size: datasetVersion1.info.size !== datasetVersion2.info.size,
    features: getKeyValuePairsDiff(
      datasetVersion1.info.features.map(feature => ({
        key: feature,
        value: feature,
      })),
      datasetVersion2.info.features.map(feature => ({
        key: feature,
        value: feature,
      }))
    ),
    numRecords:
      datasetVersion1.info.numRecords !== datasetVersion2.info.numRecords,
    objectPath:
      datasetVersion1.info.objectPath !== datasetVersion2.info.objectPath,
    checkSum: datasetVersion1.info.checkSum !== datasetVersion2.info.checkSum,
  };
};
const getQueryDatasetVersionsDifferentProps = (
  datasetVersion1: IQueryDatasetVersion,
  datasetVersion2: IQueryDatasetVersion
): IQueryDatasetVersionDifferentProps => {
  return {
    ...getDatasetVersionCommonDifferentProps(datasetVersion1, datasetVersion2),
    query: datasetVersion1.info.query !== datasetVersion2.info.query,
    queryParameters: getKeyValuePairsDiff(
      datasetVersion1.info.queryParameters.map(({ name, value }) => ({
        key: name,
        value,
      })),
      datasetVersion2.info.queryParameters.map(({ name, value }) => ({
        key: name,
        value,
      }))
    ),
    queryTemplate:
      datasetVersion1.info.queryTemplate !== datasetVersion2.info.queryTemplate,
    dataSourceUri:
      datasetVersion1.info.dataSourceUri !== datasetVersion2.info.dataSourceUri,
    executionTimestamp:
      datasetVersion1.info.executionTimestamp !==
      datasetVersion2.info.executionTimestamp,
    numRecords:
      datasetVersion1.info.numRecords !== datasetVersion2.info.numRecords,
  };
};
const getPathDatasetVersionsDifferentProps = (
  datasetVersion1: IPathBasedDatasetVersion,
  datasetVersion2: IPathBasedDatasetVersion
): IPathDatasetVersionDifferentProps => {
  return {
    ...getDatasetVersionCommonDifferentProps(datasetVersion1, datasetVersion2),
    basePath: datasetVersion1.info.basePath !== datasetVersion2.info.basePath,
    locationType:
      datasetVersion1.info.locationType !== datasetVersion2.info.locationType,
    size: datasetVersion1.info.size !== datasetVersion2.info.size,
    datasetPathInfos: compareDatasetPathInfos(
      datasetVersion1.info.datasetPathInfos,
      datasetVersion2.info.datasetPathInfos
    ),
  };
};
const compareDatasetPathInfos = (
  datasetPathInfos1: IDatasetPathPartInfo[],
  datasetPathInfos2: IDatasetPathPartInfo[]
): IDiffDatasetPathInfos => {
  const paths = R.uniq(
    datasetPathInfos1
      .map(({ path }) => path)
      .concat(datasetPathInfos2.map(({ path }) => path))
  );
  return R.fromPairs(
    paths.map(path => {
      const pathInfo1 = datasetPathInfos1.find(x => x.path === path);
      const pathInfo2 = datasetPathInfos2.find(x => x.path === path);
      if ((!pathInfo1 && pathInfo2) || (pathInfo1 && !pathInfo2)) {
        return [
          path,
          { path: true, size: true, checkSum: true, lastModified: true },
        ];
      }
      return [
        path,
        {
          path: false,
          size: pathInfo1!.size !== pathInfo2!.size,
          checkSum: pathInfo1!.checkSum !== pathInfo2!.checkSum,
          lastModified: pathInfo1!.lastModified !== pathInfo2!.lastModified,
        },
      ];
    })
  ) as any;
};

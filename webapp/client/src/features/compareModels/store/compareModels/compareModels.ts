import * as R from 'ramda';

import { Artifact, IArtifact } from 'shared/models/Artifact';
import { IAttribute } from 'shared/models/Attribute';
import {
  ICodeVersion,
  IArtifactCodeVersion,
  IGitCodeVersion,
} from 'shared/models/CodeVersion';
import {
  getKeyValuePairsDiff,
  getObjsPropsDiffByPred,
  getObjsPropsDiff,
} from 'shared/utils/collection';
import ModelRecord from 'shared/models/ModelRecord';
import { IKeyValuePair } from 'shared/models/Common';
import matchBy from 'shared/utils/matchBy';
import { makeCompareCommitsUrl } from 'shared/utils/github/github';

type ModelId = string;

export type ComparedModelRecord = ModelRecord & { modelNumber: number; };
export type ComparedMultipleModels = [ComparedModelRecord, ComparedModelRecord, ...ComparedModelRecord[]];

export const getComparedMultipleModels = <T extends ModelRecord | undefined>(models: T[]): ComparedMultipleModels | undefined  => {
  return checkIsEnableModelsComparing(models.filter(Boolean).length) ?
    (models as ModelRecord[]).map((model, i) => {
      const res: ComparedModelRecord = {
        ...model,
        modelNumber: i + 1,
      };
      return res;
    }) as ComparedMultipleModels :
    undefined;
};

export const minNumberOfModelsForComparing = 2;
export const checkIsEnableModelsComparing = (modelsNumber: number) => modelsNumber >= minNumberOfModelsForComparing;

export type IModelsDifferentProps = Record<ModelId, IModelDifferentProps>;

export interface IModelDifferentProps {
  id: boolean;
  ownerId: boolean;
  projectId: boolean;
  hyperparameters: IKeyValuePairsDiff;
  tags: boolean;
  metrics: IKeyValuePairsDiff;
  observations: boolean;
  experimentId: boolean;
  artifacts: { [key: string]: boolean };
  datasets: IDatasetsDiff;
  attributes: IAttributesDiff;
  codeVersion: CodeVersionDiffInfo | null;
}

export enum ComparedArtifactPropType {
  key = 'key',
  type = 'type',
  path = 'path',
  linkedArtifactId = 'linkedArtifactId',
}

type IKeyValuePairsDiff = Record<string, boolean>;

type ComparableModels = [ModelRecord, ModelRecord, ...ModelRecord[]];

export type IDatasetsDiff = { [key: string]: IDatasetDiff };
export type IDatasetDiff = { [T in Extract<keyof IArtifact, 'key' | 'type' | 'linkedArtifactId' | 'path'>]: boolean };

export type IAttributesDiff = Record<string, IAttributeDiff>;
export type IAttributeDiff =
  | IPrimitiveAttributeDiff
  | IListAttributeDiff;

export type IPrimitiveAttributeDiff = { type: 'primitive'; isDiff: boolean };
export type IListAttributeDiff = { type: 'list'; diffInfo: Record<string, boolean> };

export const oneOfKeyIsDiff = (diff: Record<string, boolean>) => R.values(diff).some((t) => t);

export const checkAttributeIsDiff = (attributeDiff: IAttributeDiff): boolean => {
  return matchBy(attributeDiff, 'type')({
    list: ({ diffInfo }) => oneOfKeyIsDiff(diffInfo),
    primitive: ({ isDiff }) => isDiff,
  });
};

export const compareModels = (
  models: ComparableModels
): IModelsDifferentProps => {
  return R.fromPairs(
    models.map(currentModel => {
      const otherModels = models.filter(({ id }) => currentModel.id !== id);
      const diffProps: IModelDifferentProps = {
        id: otherModels.some(({ id }) => id !== currentModel.id),
        ownerId: otherModels.some(
          ({ ownerId }) => ownerId !== currentModel.ownerId
        ),
        projectId: otherModels.some(
          ({ projectId }) => projectId !== currentModel.projectId
        ),
        hyperparameters: compareKeyValuePairs(
          currentModel.hyperparameters,
          otherModels.map(({ hyperparameters }) => hyperparameters)
        ),
        metrics: compareKeyValuePairs(
          currentModel.metrics,
          otherModels.map(({ metrics }) => metrics)
        ),
        experimentId: otherModels.some(
          ({ experimentId }) => experimentId !== currentModel.experimentId
        ),
        artifacts: compareArtifacts(
          currentModel.artifacts,
          otherModels.map(({ artifacts }) => artifacts)
        ),
        datasets: compareDatasets(
          currentModel.datasets,
          otherModels.map(({ datasets }) => datasets)
        ),
        attributes: compareAttributes(
          currentModel.attributes,
          otherModels.map(({ attributes }) => attributes)
        ),
        codeVersion: currentModel.codeVersion ? compareCodeVersions(
          currentModel.codeVersion,
          otherModels.map(({ codeVersion }) => codeVersion)
        ) : null,

        // without comparing
        tags: false,
        observations: false,
      };
      return [currentModel.id, diffProps];
    })
  );
};

const compareKeyValuePairs = <T extends string | number>(
  currentKeyValuePairs: Array<IKeyValuePair<T>>,
  otherModelsKeyValuePairs: Array<Array<IKeyValuePair<T>>>
) => {
  return compareEntitiesWithKey(
    getKeyValuePairsDiff,
    currentKeyValuePairs,
    otherModelsKeyValuePairs
  );
};

//

const compareArtifacts = (
  currentArtifacts: IArtifact[],
  otherModelsArtifacts: Array<IArtifact[]>
) => {
  return compareEntitiesWithKey(
    compareTwoModelArtifacts,
    currentArtifacts,
    otherModelsArtifacts
  );
};
const compareTwoModelArtifacts = (
  artifacts1: Artifact[],
  artifacts2: Artifact[]
) => {
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
  return R.values(ComparedArtifactPropType).some(
    prop => !R.equals(artifact1[prop], artifact2[prop])
  );
};

//

const compareAttributes = (
  currentAttributes: IAttribute[],
  otherModelsAttributes: Array<IAttribute[]>
): IAttributesDiff => {
  return R.fromPairs(
    currentAttributes.map(currentAttribute => {
      const otherAttributesWithTheSameKey = otherModelsAttributes
        .flatMap(x => x)
        .filter(otherAttr => otherAttr.key === currentAttribute.key);

      if (!Array.isArray(currentAttribute.value)) {
        const diff: IAttributeDiff = {
          type: 'primitive',
          isDiff: otherAttributesWithTheSameKey.length !== otherModelsAttributes.length || Boolean(
            otherAttributesWithTheSameKey.find(
              otherAttr => currentAttribute.value !== otherAttr.value
            )
          ),
        };
        return [currentAttribute.key, diff] as const;
      }

      const diff: IAttributeDiff = {
        type: 'list',
        diffInfo: otherAttributesWithTheSameKey.length !== otherModelsAttributes.length || otherAttributesWithTheSameKey.some(
          ({ value }) => !Array.isArray(value)
        )
          ? R.fromPairs(currentAttribute.value.map(v => [String(v), true]))
          : compareEntitiesWithKey(
              getKeyValuePairsDiff,
              currentAttribute.value.map(value => ({ key: value, value })),
              (otherAttributesWithTheSameKey as Array<
                IKeyValuePair<Array<any>>
              >).map(({ value }) => value.map(v => ({ key: v, value: v })))
            ),
      };
      return [currentAttribute.key, diff] as const;
    }) as Array<R.KeyValuePair<string, IAttributeDiff>>
  );
};

const compareEntitiesWithKey = <T extends { key: string | number }>(
  compare: (entities1: T[], entities2: T[]) => Record<string, boolean>,
  entities1: T[],
  otherModelsEntities2: Array<T[]>
): Record<string, boolean> => {
  const diffs = otherModelsEntities2.map(anotherModelEntities2 =>
    compare(entities1, anotherModelEntities2)
  );
  return R.fromPairs(
    entities1.map(
      ({ key }) =>
        [key, diffs.some(diff => diff[key])] as R.KeyValuePair<string, boolean>
    )
  );
};

//

export type CodeVersionDiffInfo =
  | IArtifactCodeVersionDiff
  | IGitCodeVersionDiff;
export type IArtifactCodeVersionDiff = {
  type: 'artifactCodeVersion';
  diffInfoByKeys: Record<string, boolean>;
};
export type IGitCodeVersionDiff = { type: 'gitCodeVersion'; diffInfoByKeys: Record<string, boolean> };

export const compareCodeVersions = (
  currentCodeVersion1: ICodeVersion,
  otherModelsCodeVersions: (ICodeVersion | undefined)[]
): CodeVersionDiffInfo => {
  return matchBy(
    currentCodeVersion1,
    'type'
  )<CodeVersionDiffInfo>({
    artifact: () => {
      const comparedKeys: Array<keyof IArtifactCodeVersion['data']> = [
        'key',
        'path',
        'type',
      ];
      const otherModelsArtifacts = otherModelsCodeVersions.filter(
        (
          otherModelCodeVersion
        ): otherModelCodeVersion is IArtifactCodeVersion =>
          otherModelCodeVersion?.type === 'artifact'
      );
      if (otherModelsArtifacts.length !== otherModelsCodeVersions.length) {
        return {
          type: 'artifactCodeVersion',
          diffInfoByKeys: R.fromPairs(comparedKeys.map(key => [key, true])),
        };
      } else {
        return {
          type: 'artifactCodeVersion',
          diffInfoByKeys: mergeDiffInfos(
            otherModelsArtifacts.map(otherModelArtifactCodeVersion =>
              getObjsPropsDiff(
                R.pick(comparedKeys, currentCodeVersion1.data),
                R.pick(comparedKeys, otherModelArtifactCodeVersion.data)
              )
            )
          ),
        };
      }
    },
    git: () => {
      const otherModelsGitCodeVersions = otherModelsCodeVersions.filter(
        (otherModelCodeVersion): otherModelCodeVersion is IGitCodeVersion =>
          otherModelCodeVersion?.type === 'git'
      );
      if (otherModelsGitCodeVersions.length !== otherModelsCodeVersions.length) {
        return {
          type: 'gitCodeVersion',
          diffInfoByKeys: R.fromPairs(
            Object.keys(currentCodeVersion1.data).map(key => [key, true])
          ),
        };
      } else {
        return {
          type: 'gitCodeVersion',
          diffInfoByKeys: mergeDiffInfos(
            otherModelsGitCodeVersions.map(otherModelArtifactCodeVersion =>
              getObjsPropsDiff(
                currentCodeVersion1.data,
                otherModelArtifactCodeVersion.data
              )
            )
          ),
        };
      }
    },
  });
};

type ModelIdWithCodeVersion = { id: string; codeVersion: ICodeVersion };
// todo rename
export const getUrlForComparingCodeVersions = (modelsCodeVersions: (ModelIdWithCodeVersion | undefined)[]) => {
  if (!is2Tuple(modelsCodeVersions)) {
    return undefined;
  }

  const [modelCodeVersion1, modelCodeVersion2] = modelsCodeVersions;

  if (!modelCodeVersion1 || !modelCodeVersion2) {
    return undefined;
  }

  if (modelCodeVersion1.codeVersion.type === 'git' && modelCodeVersion2.codeVersion.type === 'git') {
    return getUrlForComparingGitCodeVersions(modelCodeVersion1.codeVersion, modelCodeVersion2.codeVersion);
  }

  if (modelCodeVersion1.codeVersion.type === 'artifact' && modelCodeVersion2.codeVersion.type === 'artifact') {
    return getUrlForComparingArtifactCodeVersions(modelCodeVersion1, modelCodeVersion2);
  }
};

const getUrlForComparingGitCodeVersions = (gitCodeVersion1: IGitCodeVersion, gitCodeVersion2: IGitCodeVersion): string | undefined => {
  const { data: { remoteRepoUrl: remoteRepoUrl1, commitHash: commitHash1 } } = gitCodeVersion1;
  const { data: { remoteRepoUrl: remoteRepoUrl2, commitHash: commitHash2 } } = gitCodeVersion2;
  if (remoteRepoUrl1 && remoteRepoUrl2 &&
    remoteRepoUrl1.type === 'github' && remoteRepoUrl2.type === 'github' &&
    R.equals(remoteRepoUrl1.value, remoteRepoUrl2.value) &&
    commitHash1 && commitHash2) {
      return makeCompareCommitsUrl({
        repoWithCommitHash1: {
          url: remoteRepoUrl1.value,
          commitHash: commitHash1,
        },
        repoWithCommitHash2: {
          url: remoteRepoUrl2.value,
          commitHash: commitHash2,
        }
      });
  }
  return undefined;
};

const getUrlForComparingArtifactCodeVersions = (modelWithArtifactCodeVersion1: ModelIdWithCodeVersion, modelWithArtifactCodeVersion2: ModelIdWithCodeVersion) => {
  return `${process.env.REACT_APP_BACKEND_API ||
    ''}/api/v1/nbdiff/query/?base=${modelWithArtifactCodeVersion1.id}&remote=${
    modelWithArtifactCodeVersion2.id
  }`;
};

const is2Tuple = <T>(items: T[]): items is [T, T] => {
  return items.length === 2;
};

//

const compareDatasets = (currentDatasets: IArtifact[], otherModelsDatasets: Array<IArtifact[]>): IDatasetsDiff => {
  const comparedKeys: Array<keyof IDatasetDiff> = ['key', 'linkedArtifactId', 'path', 'type'];
  return R.fromPairs(currentDatasets.map((currentDataset) => {
    const otherDatasetsWithTheSameKey = otherModelsDatasets.flatMap((x) => x).filter((x) => x.key === currentDataset.key);

    
    if (otherDatasetsWithTheSameKey.length !== otherModelsDatasets.length) {
      return [currentDataset.key, R.fromPairs(comparedKeys.map(key => [key, true])) as IDatasetDiff];
    }

    return [currentDataset.key, mergeDiffInfos(
      otherDatasetsWithTheSameKey
        .map((anotherModelDataset) => getObjsPropsDiff(
          R.pick(comparedKeys, currentDataset),
          R.pick(comparedKeys, anotherModelDataset)
        )
      )
    ) as IDatasetDiff];
  }));
};

const mergeDiffInfos = <T extends Record<string, boolean>>(
  diffs: T[]
): T => {
  const [first] = diffs;
  return Object.fromEntries(
    Object.keys(first).map(key => [key, diffs.some(diff => diff[key])])
  ) as T;
};

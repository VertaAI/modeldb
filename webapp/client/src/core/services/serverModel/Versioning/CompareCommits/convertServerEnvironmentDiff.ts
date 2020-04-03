import {
  DiffType,
  ComparedCommitType,
  IElementDiff,
  elementDiffMakers,
} from 'core/shared/models/Versioning/Blob/Diff';
import {
  IEnvironmentBlobDiff,
  IEnvironmentVariablesBlob,
  IVersionEnvironmentBlob,
  IPythonRequirementEnvironment,
  IDockerEnvironmentBlob,
  IEnvironmentBlobDataDiff,
  IPythonEnvironmentBlob,
} from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import { convertServerDockerBlob } from '../RepositoryData/Blob/EnviromentBlob';
import {
  IServerBlobDiff,
  IServerElementDiff,
  convertServerBlobDiffToClient,
} from './ServerDiff';
import matchType from 'core/shared/utils/matchType';

export const convertServerEnvironmentDiff = (
  serverConfigDiff: IServerEnvironmentDiff
): IEnvironmentBlobDiff => {
  return convertServerBlobDiffToClient(
    {
      convertData: ({ environment }, { diffType }) => {
        return matchType<DiffType, IElementDiff<IEnvironmentBlobDataDiff>>(
          {
            added: () =>
              elementDiffMakers.added(convertToDiffBlobData('B', environment)),
            deleted: () =>
              elementDiffMakers.deleted(
                convertToDiffBlobData('A', environment)
              ),
            modified: () =>
              elementDiffMakers.modified(
                convertToDiffBlobData('A', environment),
                convertToDiffBlobData('B', environment)
              ),
          },
          diffType
        );
      },
      category: 'environment',
      type: 'environment',
    },
    serverConfigDiff
  );
};

const convertToDiffBlobData = (
  type: ComparedCommitType,
  environment: IServerEnvironmentDiff['environment']
): IEnvironmentBlobDataDiff => {
  return {
    commandLine:
      environment.command_line && (environment.command_line as any)[type],
    variables:
      environment.environment_variables &&
      environment.environment_variables
        .map((v: any) => v[type])
        .filter(Boolean),
    data: environment.docker
      ? convertServerDockerBlob(
          (environment.docker as any).A || (environment.docker as any).B
        )
      : environment.python
      ? convertServerPythonDiffToBlob(type, environment.python)
      : undefined,
  };
};

const convertServerPythonDiffToBlob = (
  type: ComparedCommitType,
  serverPythonDiff: IServerPythonDiff
): IPythonEnvironmentBlob => {
  return {
    type: 'python',
    data: {
      pythonVersion:
        serverPythonDiff.version && (serverPythonDiff.version as any)[type],
      constraints:
        serverPythonDiff.constraints &&
        serverPythonDiff.constraints.map(d => (d as any)[type]).filter(Boolean),
      requirements:
        serverPythonDiff.requirements &&
        serverPythonDiff.requirements
          .map(d => (d as any)[type])
          .filter(Boolean),
    },
  };
};

export type IServerEnvironmentDiff = IServerBlobDiff<{
  environment: {
    python?: IServerPythonDiff;
    docker?: IServerElementDiff<IDockerEnvironmentBlob>;

    environment_variables?: Array<
      IServerElementDiff<IEnvironmentVariablesBlob>
    >;
    command_line?: IServerElementDiff<string[]>;
  };
}>;

type IServerPythonDiff = {
  version: IServerElementDiff<IVersionEnvironmentBlob>;

  requirements: Array<IServerElementDiff<IPythonRequirementEnvironment>>;
  constraints: Array<IServerElementDiff<IPythonRequirementEnvironment>>;
};

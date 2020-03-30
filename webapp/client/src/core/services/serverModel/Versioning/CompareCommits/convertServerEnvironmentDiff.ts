import {
  DiffType,
  ComparedCommitType,
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
import { IServerBlobDiff, IServerElementDiff } from './ServerDiff';
import { DataLocation } from 'core/shared/models/Versioning/DataLocation';

export const convertServerEnvironmentDiff = (
  serverConfigDiff: IServerEnvironmentDiff,
  diffType: DiffType
): IEnvironmentBlobDiff => {
  const { environment } = serverConfigDiff;
  switch (serverConfigDiff.status) {
    case 'ADDED': {
      return {
        category: 'environment',
        type: 'environment',
        diffType: 'added',
        location: serverConfigDiff.location as DataLocation,
        data: {
          diffType: 'added',
          B: convertToDiffBlobData('B', environment),
        },
      };
    }
    case 'DELETED': {
      return {
        category: 'environment',
        type: 'environment',
        diffType: 'deleted',
        location: serverConfigDiff.location as DataLocation,
        data: {
          diffType: 'deleted',
          A: convertToDiffBlobData('A', environment),
        },
      };
    }

    case 'MODIFIED': {
      return {
        category: 'environment',
        type: 'environment',
        diffType: 'updated',
        location: serverConfigDiff.location as DataLocation,
        data: {
          diffType: 'updated',
          A: convertToDiffBlobData('A', environment),
          B: convertToDiffBlobData('B', environment),
        },
      };
    }

    default:
      throw new Error('is not handled!');
  }
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

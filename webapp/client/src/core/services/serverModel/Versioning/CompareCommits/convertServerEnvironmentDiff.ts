import { DiffType } from 'core/shared/models/Versioning/Blob/Diff';
import { IEnvironmentBlobDiff } from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import {
  convertServerDockerBlob,
  convertServerPythonBlob,
} from '../RepositoryData/Blob/EnviromentBlob';

export const convertServerEnvironmentDiff = (
  serverConfigDiff: any,
  diffType: DiffType
): IEnvironmentBlobDiff => {
  const { location, environment } = serverConfigDiff;

  switch (diffType) {
    case 'added':
    case 'deleted': {
      return {
        category: 'environment',
        type: 'environment',
        diffType,
        location,
        blob: {
          commandLine: environment.command_line_B || environment.command_line_A,
          variables:
            environment.environment_variables_B ||
            environment.environment_variables_A,
          data: environment.docker
            ? convertServerDockerBlob(
                environment.docker.A || environment.docker.B
              )
            : environment.python
            ? (convertServerPythonBlob(
                environment.python.A || environment.python.B
              ) as any)
            : null,
        },
      };
    }

    case 'updated': {
      return {
        category: 'environment',
        type: 'environment',
        diffType: 'updated',
        location,
        blobA: {
          commandLine: environment.command_line_A,
          variables: environment.environment_variables_A,
          data: environment.docker
            ? convertServerDockerBlob(environment.docker.A)
            : environment.python
            ? (convertServerPythonBlob(environment.python.A) as any)
            : null,
        },
        blobB: {
          commandLine: environment.command_line_B,
          variables: environment.environment_variables_B,
          data: environment.docker
            ? convertServerDockerBlob(environment.docker.B)
            : environment.python
            ? (convertServerPythonBlob(environment.python.B) as any)
            : null,
        },
      };
    }

    default:
      throw new Error('is not handled!');
  }
};

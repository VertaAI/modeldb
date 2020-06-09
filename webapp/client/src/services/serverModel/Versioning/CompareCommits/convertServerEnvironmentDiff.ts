import {
  IEnvironmentBlobDiff,
  IEnvironmentVariablesBlob,
  IVersionEnvironmentBlob,
  IPythonRequirementEnvironment,
  IDockerEnvironmentBlob,
  IDockerEnvironmentBlobDiff,
  IPythonEnvironmentBlobDiff,
} from 'core/shared/models/Versioning/Blob/EnvironmentBlob';
import {
  IServerBlobDiff,
  IServerElementDiff,
  convertServerBlobDiffToClient,
  convertServerElementDiffToClient,
  convertNullableServerElementDiffToClient,
  convertNullableServerArrayDiffToClient,
} from './ServerDiff';

export const convertServerEnvironmentDiff = (
  serverConfigDiff: IServerEnvironmentDiff
): IEnvironmentBlobDiff => {
  return convertServerBlobDiffToClient(
    {
      convertData: ({ environment }) => {
        return {
          commandLine: convertNullableServerElementDiffToClient(
            (x: string[]) => x,
            environment.command_line
          ),
          variables: convertNullableServerArrayDiffToClient(
            (x: IEnvironmentVariablesBlob) => x,
            environment.environment_variables
          ),
          data: (() => {
            if (environment.python) {
              const res: IPythonEnvironmentBlobDiff = {
                type: 'python',
                data: {
                  constraints: convertNullableServerArrayDiffToClient(
                    (x: IPythonRequirementEnvironment) => x,
                    environment.python.constraints
                  ),
                  requirements: convertNullableServerArrayDiffToClient(
                    (x: IPythonRequirementEnvironment) => x,
                    environment.python.requirements
                  ),
                  pythonVersion: convertNullableServerElementDiffToClient(
                    (x: IVersionEnvironmentBlob) => x,
                    environment.python.version
                  ),
                },
              };
              return res;
            }
            if (environment.docker) {
              const res: IDockerEnvironmentBlobDiff = {
                type: 'docker',
                data: convertServerElementDiffToClient(
                  (d: IDockerEnvironmentBlob['data']) => d,
                  environment.docker
                ),
              };
              return res;
            }
            return undefined;
          })(),
        };
      },
      category: 'environment',
      type: 'environment',
    },
    serverConfigDiff
  );
};

export type IServerEnvironmentDiff = IServerBlobDiff<{
  environment: {
    python?: IServerPythonDiff;
    docker?: IServerElementDiff<IDockerEnvironmentBlob['data']>;

    environment_variables?: Array<
      IServerElementDiff<IEnvironmentVariablesBlob>
    >;
    command_line?: IServerElementDiff<string[]>;
  };
}>;

type IServerPythonDiff = {
  version?: IServerElementDiff<IVersionEnvironmentBlob>;

  requirements?: Array<IServerElementDiff<IPythonRequirementEnvironment>>;
  constraints?: Array<IServerElementDiff<IPythonRequirementEnvironment>>;
};

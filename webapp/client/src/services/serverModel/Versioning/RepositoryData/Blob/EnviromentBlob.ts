import {
  IEnvironmentBlob,
  IDockerEnvironmentBlob,
  IPythonEnvironmentBlob,
} from 'shared/models/Versioning/Blob/EnvironmentBlob';

export const convertServerEnvironmentBlob = (
  serverEnvironment: any
): IEnvironmentBlob => {
  if (serverEnvironment.python) {
    return {
      category: 'environment',
      data: {
        data: convertServerPythonBlob(serverEnvironment.python),
        commandLine: serverEnvironment.command_line,
        variables: serverEnvironment.environment_variables,
      },
    };
  }

  if (serverEnvironment.docker) {
    return {
      category: 'environment',
      data: {
        data: convertServerDockerBlob(serverEnvironment.docker),
        commandLine: serverEnvironment.command_line,
        variables: serverEnvironment.environment_variables,
      },
    };
  }

  throw new Error(`${serverEnvironment} unknown environment type`);
};

export const convertServerPythonBlob = (
  serverPython: any
): IPythonEnvironmentBlob => {
  return {
    type: 'python',
    data: {
      requirements: serverPython.requirements,
      constraints: serverPython.constraints,
      pythonVersion: serverPython.version,
    },
  };
};

export const convertServerDockerBlob = (
  serverDocker: any
): IDockerEnvironmentBlob => {
  return {
    type: 'docker',
    data: {
      repository: serverDocker.repository,
      sha: serverDocker.sha,
      tag: serverDocker.tag,
    },
  };
};

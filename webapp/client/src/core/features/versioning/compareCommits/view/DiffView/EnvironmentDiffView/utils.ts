import {
  IPythonRequirementEnvironment,
  IVersionEnvironmentBlob,
  IDockerEnvironmentBlob,
  IEnvironmentVariablesBlob,
  IEnvironmentBlobDiff,
} from 'core/shared/models/Versioning/Blob/EnvironmentBlob';

import {
  getCssDiffColor,
  IArrayDiffViewModel,
  IElementDiffViewModel,
  getArrayDiffViewModel,
  getElementDiffViewModel,
  IArrayElementDiffViewModel,
} from '../../model';
import { ComparedCommitType } from 'core/shared/models/Versioning/Blob/Diff';

export interface IEnvironmentDiffViewModel {
  commonDetails: IEnvironmentCommonDetailsDiffViewModel;
  data?: IPythonEnvironmentDiffViewModel | IDockerEnvironmentDiffViewModel;
}

export interface IPythonEnvironmentDiffViewModel {
  type: 'python';
  data: {
    requirements: IArrayDiffViewModel<IPythonRequirementEnvironment>;
    constraints: IArrayDiffViewModel<IPythonRequirementEnvironment>;
    pythonVersion: IElementDiffViewModel<IVersionEnvironmentBlob>;
  };
}

export interface IDockerEnvironmentDiffViewModel {
  type: 'docker';
  data: IElementDiffViewModel<IDockerEnvironmentBlob['data']>;
}

export interface IEnvironmentCommonDetailsDiffViewModel {
  isHidden: boolean;
  environmentVariables: IArrayDiffViewModel<IEnvironmentVariablesBlob>;
  commandLine: IElementDiffViewModel<string[]>;
}

export const getEnvironmentDiffViewModel = (
  diff: IEnvironmentBlobDiff
): IEnvironmentDiffViewModel => {
  return {
    commonDetails: {
      isHidden: Boolean(!diff.data.variables && !diff.data.commandLine),
      environmentVariables: getArrayDiffViewModel(
        ({ name }) => name,
        diff.data.variables
      ),
      commandLine: getElementDiffViewModel(diff.data.commandLine),
    },

    data: (() => {
      const pythonOrDocker = diff.data.data;

      if (!pythonOrDocker) {
        return undefined;
      }

      return pythonOrDocker.type === 'python'
        ? {
            type: 'python' as const,
            data: {
              pythonVersion: getElementDiffViewModel(
                pythonOrDocker.data.pythonVersion
              ),
              constraints: getArrayDiffViewModel(
                ({ library }) => library,
                pythonOrDocker.data.constraints
              ),
              requirements: getArrayDiffViewModel(
                ({ library }) => library,
                pythonOrDocker.data.requirements
              ),
            },
          }
        : {
            type: 'docker' as const,
            data: getElementDiffViewModel(pythonOrDocker.data),
          };
    })(),
  };
};

export const getArrayElemDiffStyles = (
  elem: IArrayElementDiffViewModel<any>
) => {
  const styles: React.CSSProperties = {
    backgroundColor: getCssDiffColor(elem.diffColor),
  };
  if (elem.hightlightedPart === 'full') {
    return {
      rootStyles: styles,
    };
  } else {
    return { valueStyles: styles };
  }
};

export const needHighlightCellBackground = (arrayViewModel: Array<IArrayElementDiffViewModel<any>> | undefined) => {
  if (arrayViewModel && arrayViewModel.length > 0) {
    return arrayViewModel.every(({ diffColor }) => diffColor === 'green') || arrayViewModel.every(({ diffColor }) => diffColor === 'red');
  }
  return false;
};

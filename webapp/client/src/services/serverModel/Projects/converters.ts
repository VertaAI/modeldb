import { JsonConvert } from 'json2typescript';
import { Project, ProjectVisibility } from 'models/Project';
import { convertServerCodeVersion } from '../CodeVersion/converters';
import { convertServerEntityWithLoggedDates } from '../Common/converters';
import { convertServerShortWorkspaceToClient } from '../Workspace/converters';

export const convertHydratedProjectToClient = (
  serverHydratedProject: any
): Project => {
  const jsonConvert = new JsonConvert();
  const project: Project = jsonConvert.deserializeObject(
    serverHydratedProject.project,
    Project
  );

  if (serverHydratedProject.project.code_version_snapshot) {
    project.codeVersion = convertServerCodeVersion(
      serverHydratedProject.project.code_version_snapshot
    );
  }

  const dates = convertServerEntityWithLoggedDates(
    serverHydratedProject.project
  );
  project.dateCreated = dates.dateCreated;
  project.dateUpdated = dates.dateUpdated;

  project.shortWorkspace = convertServerShortWorkspaceToClient(
    serverHydratedProject.project
  );

  return project;
};

export const convertProjectVisibilityToServer = (
  projectVisibility: ProjectVisibility
): number => {
  const record: Record<ProjectVisibility, number> = {
    private: 0,
  };
  return record[projectVisibility];
};

import { Project } from 'models/Project';

import { userWorkspacesWithCurrentUser } from './workspace';

export const makeProject = ({
  id,
  name,
  shortWorkspace = userWorkspacesWithCurrentUser.user,
}: {
  id: Project['id'];
  name: Project['name'];
  description?: Project['description'];
  tags?: Project['tags'];
  shortWorkspace?: Project['shortWorkspace'];
}) => {
  const mockProject: Project = new Project();
  mockProject.id = id;
  mockProject.name = name;
  mockProject.shortWorkspace = shortWorkspace;
  return mockProject;
};

export const currentUserProjects = [
  makeProject({
    id: 'project-id-1',
    name: 'project-name-1',
  }),
  makeProject({
    id: 'project-id-2',
    name: 'project-name-2',
  }),
];

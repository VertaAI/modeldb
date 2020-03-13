import { Dataset } from 'models/Dataset';

import { userWorkspacesWithCurrentUser } from './workspace';

export const makeDataset = ({
  id,
  name,
  description,
  attributes = [],
  tags = [],
  type,
  shortWorkspace = userWorkspacesWithCurrentUser.user,
}: {
  id: Dataset['id'];
  name: Dataset['name'];
  attributes?: Dataset['attributes'];
  description?: Dataset['description'];
  tags?: Dataset['tags'];
  shortWorkspace?: Dataset['shortWorkspace'];
  type: Dataset['type'];
}): Dataset => {
  return {
    id,
    name,
    type,
    description: description || 'description',
    attributes,
    dateCreated: new Date(),
    dateUpdated: new Date(),
    isPubliclyVisible: false,
    tags,
    shortWorkspace,
  } as Dataset;
};

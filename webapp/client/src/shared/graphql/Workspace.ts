import gql from 'graphql-tag';

export const WORKSPACE_FRAGMENT = gql`
  fragment WorkspaceData on Workspace {
    id: name
  }
`;

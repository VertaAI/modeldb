import gql from 'graphql-tag';
import { useMutation } from 'react-apollo';

import { mutationResultToCommunication } from 'core/shared/utils/graphql/queryResultToCommunicationWithData';

import * as AddLabelTypes from './graphql-types/AddLabel';
import * as DeleteLabelTypes from './graphql-types/DeleteLabel';
import makeTuple from 'core/shared/utils/makeTuple';

const ADD_LABEL = gql`
  mutation AddLabel($id: ID!, $label: String!) {
    repository(id: $id) {
      id
      addLabels(labels: [$label]) {
        id
        labels
      }
    }
  }
`;
export function useAddLabelMutation() {
  const [addLabelMutation, mutationResult] = useMutation<
    AddLabelTypes.AddLabel,
    AddLabelTypes.AddLabelVariables
  >(ADD_LABEL);
  const addLabel = (variables: AddLabelTypes.AddLabelVariables) =>
    addLabelMutation({ variables });

  return makeTuple(addLabel, mutationResultToCommunication(mutationResult));
}

const DELETE_LABEL = gql`
  mutation DeleteLabel($id: ID!, $label: String!) {
    repository(id: $id) {
      id
      deleteLabels(labels: [$label]) {
        id
        labels
      }
    }
  }
`;
export function useDeleteLabelMutation() {
  const [deleteLabelMutation, mutationResult] = useMutation<
    DeleteLabelTypes.DeleteLabel,
    DeleteLabelTypes.DeleteLabelVariables
  >(DELETE_LABEL);
  const deleteLabel = (variables: DeleteLabelTypes.DeleteLabelVariables) =>
    deleteLabelMutation({ variables });

  return makeTuple(deleteLabel, mutationResultToCommunication(mutationResult));
}

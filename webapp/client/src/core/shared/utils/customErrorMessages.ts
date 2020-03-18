// Note: No text formatting applied on UI to maintain case-sensitivity
// to be consistent with this catalog, please apply text case here to translate into UI

const capitalize = (s: string) => {
  return s.charAt(0).toUpperCase() + s.slice(1);
};

export const defaultErrorMessages = {
  // error codes
  default_error_code: '',
  default_client_error_code: 404,
  default_server_error_code: 500,

  // global default messages
  default_error: 'Unknown Error!',
  page_error: 'Page not found.',

  // api errors
  client_error_4xx: `Bad Request!`,
  server_error_5xx: `There was a problem serving your request!`,
  default_api_error: 'Please try again. Something went wrong!',

  // code error
  default_code_error: `Something went wrong!`,
};

export const commonAPIErrorMessages = {
  userNotFound: 'User not found for the given email.',

  // Entity error messages
  accessDeniedToEntity: 'Access denied to entity.',
  entityNotFound: 'Entity not found.',
};

export const artifactErrorMessages = {
  // Custom component error message to give more context to the user
  // Artifacts
  artifact_download: 'Error in downloading file.',
  artifact_deleting: 'Error in deleting file.',
  artifact_preview: 'Error in loading preview:',
};

export const UIValidationErrorMessage = {
  common: {
    empty: (field: string) => `${capitalize(field)} is empty!`,
    email: `Invalid Email!`,
    minLength: (min: number) =>
      `Length should be at least ${min} characters long`,
    maxLength: (max: number) =>
      `Length should not be more then ${max} characters long`,
    containAtLeastOne: (symbol: string) =>
      `Should contain at least one ${symbol}`,
    lengthLessOrEqualThen: (n: number, field: string) =>
      `${capitalize(field)} must be less ${n} characters`,
    lengthLessThen: (n: number, field: string) =>
      `Max length of ${field} is ${n} chars!`,
    symbols: (symbolNames: string[]) =>
      `User name can contain only ${symbolNames.join(', ')}!`,
  },
};

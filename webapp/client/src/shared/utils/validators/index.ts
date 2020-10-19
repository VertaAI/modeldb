import { UIValidationErrorMessage } from '../customErrorMessages';

export const validateNotEmpty = (type: string) => (value: string) => {
  return value === '' || value === null || value === undefined
    ? UIValidationErrorMessage.common.empty(type)
    : undefined;
};

export const validateEmailNotEmpty = validateNotEmpty('Email');

export const validateEmail = (email: string) => {
  // tslint:disable-next-line: max-line-length
  const re = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
  return !re.test(String(email).toLowerCase())
    ? UIValidationErrorMessage.common.email
    : undefined;
};

export function skipWhenEmpty<Value extends string, Error = string>(
  validator: Validator<Value, Error>
) {
  return (value: Value) => {
    return validateNotEmpty('')(value) ? undefined : validator(value);
  };
}

export function combineValidators<Value, Error = string>(
  validators: Array<Validator<Value, Error>>
): Validator<Value, Error> {
  return (value: Value) =>
    validators.reduce((maybeError, validator) => {
      return maybeError !== undefined ? maybeError : validator(value);
    }, undefined as Error | undefined);
}

export const validateMinLength = (min: number) => (value: string) =>
  value.length < min
    ? UIValidationErrorMessage.common.minLength(min)
    : undefined;

export const validateMaxLength = (max: number) => (value: string) =>
  value.length >= max
    ? UIValidationErrorMessage.common.maxLength(max)
    : undefined;

export const validateContainAtLeastOne = (type: string, regexp: RegExp) => (
  value: string
) =>
  !new RegExp(regexp, 'g').test(value)
    ? UIValidationErrorMessage.common.containAtLeastOne(type)
    : undefined;

export const validateLengthLessOrEqualThen = (n: number, fieldName: string) => (
  field: string
): string | undefined => {
  return field.length >= n
    ? UIValidationErrorMessage.common.lengthLessOrEqualThen(n, fieldName)
    : undefined;
};

export const validateLengthLessThen = (n: number, fieldName: string) => (
  field: string
): string | undefined => {
  return field.length > n
    ? UIValidationErrorMessage.common.lengthLessThen(n, fieldName)
    : undefined;
};

export const validateSymbols = (symbolNames: string[]) => (value: string) =>
  !/^[a-zA-Z0-9_-]*$/.test(value)
    ? UIValidationErrorMessage.common.symbols(symbolNames)
    : undefined;

export type Validator<Value, Error = string> = (
  value: Value
) => Error | undefined;

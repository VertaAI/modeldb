export type NonEmptyString = string & { __brand: 'NonEmptyString' };

function nonEmptyString(str: ''): never;
function nonEmptyString(str: string): NonEmptyString;
function nonEmptyString(str: string): NonEmptyString {
  if (str === '') {
    throw new TypeError('empty string passed to nonEmptyString()');
  }
  return str as NonEmptyString;
}
export { nonEmptyString };

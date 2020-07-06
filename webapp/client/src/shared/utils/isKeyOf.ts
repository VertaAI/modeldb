export const isKeyOf = <T extends object>(
  object: T,
  key: string | number | symbol
): key is keyof T => {
  if (key in object) {
    return true;
  }
  return false;
};

export function exhaustiveCheck(check: never, message: string): never {
  return message as never;
}

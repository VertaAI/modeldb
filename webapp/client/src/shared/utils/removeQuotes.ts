export default function removeQuotes(string: string) {
  return string.replace(/['"]+/g, '');
}

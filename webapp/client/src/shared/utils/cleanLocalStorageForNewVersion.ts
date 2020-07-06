export default function cleanLocalStorageForNewVersion(
  currentLocalStorageVersion: string
) {
  const prevLocalStorageVersion = localStorage.getItem('localStorageVersion');
  if (
    !prevLocalStorageVersion ||
    prevLocalStorageVersion !== currentLocalStorageVersion
  ) {
    localStorage.clear();
    localStorage.setItem('localStorageVersion', currentLocalStorageVersion);
    return;
  }
}

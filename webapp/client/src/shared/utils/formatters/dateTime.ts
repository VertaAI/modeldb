const options = {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
};

export const getFormattedDate = (dt: Date | string | number) => {
  if (typeof dt === 'string') {
    dt = Number(dt);
  }
  const date = new Date(dt);
  return date.toLocaleDateString('default', options);
};

export const getFormattedTime = (dt: Date | string | number) => {
  if (typeof dt === 'string') {
    dt = Number(dt);
  }
  const date = new Date(dt);
  return date.toLocaleTimeString();
};

export const getFormattedDateTime = (dt: Date | string | number) => {
  if (typeof dt === 'string') {
    dt = Number(dt);
  }
  const date = new Date(dt);
  return date.toLocaleDateString('default', options);
};

# build environment

FROM node:12.16.2-alpine as builder
# Create working directory
RUN mkdir -p /usr/src/app/client

# Install required package dependencies
WORKDIR /usr/src/app/client
COPY client/package.json /usr/src/app/client/package.json
COPY client/yarn.lock /usr/src/app/client/yarn.lock
RUN yarn install

# Copy app source
WORKDIR /usr/src/app/client
COPY client /usr/src/app/client
ENV REACT_APP_OSS 1
ENV REACT_APP_PUBLIC_URL /
RUN yarn build

FROM node:12.16.2-alpine

RUN mkdir -p /usr/src/app/client
WORKDIR /usr/src/app

COPY package.json /usr/src/app/package.json
COPY yarn.lock /usr/src/app/yarn.lock
RUN yarn install

COPY server.js server.js
COPY .env.default .env

COPY --from=builder /usr/src/app/client/build /usr/src/app/client/build
ENV DEPLOYED "yes"

# production environment
EXPOSE 3000
CMD [ "yarn", "start" ]

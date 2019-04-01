# build environment

FROM node:latest
# Create working directory
RUN mkdir /usr/src/app
RUN mkdir /usr/src/app/client

# Install required package dependencies
WORKDIR /usr/src/app
COPY package*.json /usr/src/app/package.json
COPY yarn.lock /usr/src/app/yarn.lock
RUN yarn install

WORKDIR /usr/src/app/client
COPY client/package*.json /usr/src/app/client/package.json
COPY client/yarn.lock /usr/src/app/client/yarn.lock
RUN yarn install

# Copy app source
WORKDIR /usr/src/app
COPY . /usr/src/app
WORKDIR /usr/src/app/client
RUN cp .env.default .env
RUN yarn build

WORKDIR /usr/src/app
RUN cp .env.default .env

# production environment
EXPOSE 3000
CMD [ "yarn", "start" ]

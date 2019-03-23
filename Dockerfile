# build environment

FROM node:latest
# Create working directory
RUN mkdir /usr/src/app
WORKDIR /usr/src/app

# Install required package dependencies
COPY package*.json /usr/src/app/package.json
RUN yarn install

# Copy app source
COPY . /usr/src/app
WORKDIR /usr/src/app/client
RUN cp .env.default .env
RUN yarn install
RUN yarn build

WORKDIR /usr/src/app
RUN cp .env.default .env

# production environment
EXPOSE 3000
CMD [ "yarn", "start" ]
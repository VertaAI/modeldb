# Verta: WebApp

Frontend for ModelDB version 2

## Directory Structure

### `/`

This directory contains proxy-server for development

### `client/`

This subdirectory contains the actual frontend

### `e2e/`

This subdirectory contains e2e tests

## Setup environment for proxy-server

### Install node

`curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.29.0/install.sh | bash`

`nvm install 11.3.0`

`nvm alias default 11.3.0`

`nvm use default`

### Install yarn

<https://yarnpkg.com/en/docs/install>

**Ubuntu**:

`curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -`

`echo "deb https://dl.yarnpkg.com/debian/ stable main" | sudo tee /etc/apt/sources.list.d/yarn.list`

`sudo apt-get update && sudo apt-get install yarn`

### Install dependencies

`yarn install`

## Available Scripts

In the project directory, you can run:

### `yarn start`

Runs the proxy server on the 3000 PORT.

Note, .env file with `BACKEND_API_PROTOCOL` and `BACKEND_API_DOMAIN` is required.
Example:

```yaml
BACKEND_API_PROTOCOL='https'
BACKEND_API_DOMAIN='app.verta.ai'
```

### `yarn start-with-client`

Runs the proxy server on the 3000 PORT and the frontend in the development mode on 3001 PORT.

Note, .env file with `BACKEND_API_PROTOCOL` and `BACKEND_API_DOMAIN` is required.
Example:

```yaml
BACKEND_API_PROTOCOL='https'
BACKEND_API_DOMAIN='app.verta.ai'
```

### `yarn test`

Launches the test runner in the interactive watch mode of the frontend.
See the section about [running tests](https://facebook.github.io/create-react-app/docs/running-tests) for more information.

### `yarn pretty`

Run `prettier` for code formatting.

# Iterating in Docker Compose

When running as part of the [docker-compose-all.yaml](../docker-compose-all.yaml), the image entry for the `modeldb-frontend` must be changed to `vertaaiofficial/modeldb-frontend:latest`. Running `./build.sh` will rebuild the container and update the local docker context with a new latest.

# Working behind a path proxy

The following variables need to be set, and probably to the same path.

 - `BACKEND_API_BASE_PATH` in [.env.default](../.env.default)
 - `REACT_APP_PUBLIC_URL` in [Dockerfile](Dockerfile)
 - `"homepage"` in [package.json](package.json) 

Rebuild the modeldb-frontend container and restart.

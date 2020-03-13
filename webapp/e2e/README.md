# Verta: WebApp E2E tests

E2E tests for the webapp

## Setup environment

### Install node

```
curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.29.0/install.sh | bash
nvm install 11.3.0
nvm alias default 11.3.0
nvm use default
```

### Install yarn

https://yarnpkg.com/en/docs/install

**Ubuntu**:

```
curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -
echo "deb https://dl.yarnpkg.com/debian/ stable main" | sudo tee /etc/apt/sources.list.d/yarn.list
sudo apt-get update && sudo apt-get install yarn
```

### Install dependencies

`yarn install`

## Available Scripts

In the project directory, you can run:

### `yarn test`

Runs the e2e tests for webapp.

Note, you should have the chrome version of 77.0.0
Or you can install the chromedriver package with the version which match your the chrome version

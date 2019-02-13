# build environment

FROM node:9.6.1 as builder
# Create working directory
RUN mkdir /usr/src/app
WORKDIR /usr/src/app

# Install required package dependencies
COPY package.json /usr/src/app/package.json
RUN npm install --silent


# Get AWS CLI
RUN apt-get update && \
    apt-get install -y \
        python3 \
        python3-pip \
        python3-setuptools \
        groff \
        less \
		jq \
    && pip3 install --upgrade pip \
    && apt-get clean

RUN pip3 install awscli


ARG AWS_ACCESS_KEY_ID_BUILD
ARG AWS_SECRET_ACCESS_KEY_BUILD
ARG AWS_DEFAULT_REGION_BUILD
ARG AWS_DEFAULT_OUTPUT_BUILD
ARG SECRETS_BUILD


ENV AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID_BUILD
ENV AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY_BUILD
ENV AWS_DEFAULT_REGION=$AWS_DEFAULT_REGION_BUILD
ENV AWS_DEFAULT_OUTPUT=$AWS_DEFAULT_OUTPUT_BUILD
ENV SECRETS=$SECRETS_BUILD

# Inject environment variables
COPY inject_env.sh /usr/bin/inject_env
RUN chmod +x /usr/bin/inject_env
RUN /usr/bin/inject_env

# Copy app source
COPY . /usr/src/app
RUN npm run build

# production environment

FROM nginx:1.13.9-alpine
COPY --from=builder /usr/src/app/build /usr/share/nginx/html
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]



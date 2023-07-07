# GDX Data Share Platform

Cross governmental data sharing platform, to simplify data acquisition and sharing in an event driven way.

## Documentation
Techdocs for this service are available [here](https://alphagov.github.io/di-data-life-events-platform/).
There are also [Swagger docs](https://dev.share-life-events.service.gov.uk/swagger-ui.html).

## Running the service

### Running locally

See [contributing](CONTRIBUTING.md) for more info on running the service locally for development.

For running locally against docker instances of the following services:

- run this application independently e.g. in IntelliJ

`docker-compose -f docker-compose-local.yml up`

### Running all services including this service

`docker-compose up`

### Running remotely

The service is deployed to AWS, accessible through

| environment | url                                                                  |
|-------------|----------------------------------------------------------------------|
| dev         | https://dev.share-life-events.service.gov.uk/swagger-ui/index.html   |
| demo        | https://demo.share-life-events.service.gov.uk/swagger-ui/index.html  |
| prod        | https://share-life-events.service.gov.uk/swagger-ui/index.html       |

## Working with SAM

### Setup
To set up sam and be able to run it in the correct environment, run the following commands:

Run these
```shell
brew install jenv
echo '# Add jenv to PATH and initialise' >> ~/.zshrc
echo 'export PATH="$HOME/.jenv/bin:$PATH"' >> ~/.zshrc
echo 'eval "$(jenv init -)"' >> ~/.zshrc
echo '' >> ~/.zshrc

brew install aws-sam-cli

brew tap homebrew/cask-versions
brew install corretto17
```
Restart terminal then run
```shell
jenv add /Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home/
jenv global corretto64-17.0.7
```
Restart terminal then run in the project top directory
```shell
sam build
```

## Architecture

Architecture decision records start [here](doc/architecture/decisions/0001-use-adr.md)

## Glossary

See [TechDocs](https://alphagov.github.io/di-data-life-events-platform/glossary.html).

## Licence
[MIT License](LICENCE)

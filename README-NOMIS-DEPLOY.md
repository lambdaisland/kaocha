# How to Deploy

## Update pom.xml

`clj -Spom`

## Create jar file:

`rm -rf target ; clj -X:jar :jar target/the-jar-file.jar`

## Local deploy:

`clj -X:install`

## Clojars deploy:

`env CLOJARS_USERNAME=xxxx CLOJARS_PASSWORD=xxxx clj -X:deploy`

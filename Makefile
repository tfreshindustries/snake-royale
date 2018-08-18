build:
	docker build -t snakes/builder . -f ci/Dockerfile.builder
	docker run -it -v `pwd`:/usr/src/snakes snakes/builder
	docker build -t snakes/server:`git rev-parse --short HEAD` . -f ci/Dockerfile.server

run: build
	docker run -p 8080:80 snakes/server:`git rev-parse --short HEAD`

publish: build
	docker push snakes/server:`git rev-parse --short HEAD`

deploy: publish
	kubectl set image deployment/prod-snakes-server server=snakes/server:`git rev-parse --short HEAD`

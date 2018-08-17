build:
	docker build -t snakesgg/builder . -f Dockerfile.builder
	docker run -it \
		-v `pwd`:/usr/src/snakes \
		-v /usr/src/snakes/target \
		-v /usr/src/snakes/common/target \
		-v /usr/src/snakes/royale/target \
		-v /usr/src/snakes/server/target \
		snakesgg/builder
	docker build -t snakesgg/server:`git rev-parse --short HEAD` .

run: build
	docker run -p 8080:80 snakesgg/server:`git rev-parse --short HEAD`

publish: build
	docker push snakesgg/server:`git rev-parse --short HEAD`

deploy: publish
	echo "TODO: use awscli to deploy new image"

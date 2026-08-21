# Carries everything the demo stack needs that is authored in this repo: the device
# simulator, the seed SQL, and the ConfigProvider seed.
#
# It exists because a deployment's compose file lives outside this repository while
# these assets are versioned alongside the code that consumes them. Publishing them as
# an image from the same commit as kirpi4ik/myhab means a deployment pins two tags and
# needs no knowledge of this repo's layout.
#
# One image, two roles (see docker-entrypoint.sh):
#   init   copy the seed SQL into the database's init directory and build the bare
#          config repo, then exit. Runs once, before the app starts.
#   sim    run the device simulator against the broker. Long-running.
#
# Built by .github/workflows/gradle.yml. Build context is the repository root:
#   docker build -f demo/Dockerfile -t kirpi4ik/myhab-demo:<version> .

FROM eclipse-temurin:17-jre

# git: the ConfigProvider clones its configuration from a repository, so `init` has to
# create one. postgresql-client is not needed - the database image runs the SQL itself.
RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /demo

# Built by `./gradlew :demo-simulator:fatJar`.
COPY demo/simulator/build/libs/*-all.jar /demo/simulator.jar
COPY demo/seed/ /demo/seed/
COPY demo/config/ /demo/config/
COPY demo/docker-entrypoint.sh /demo/docker-entrypoint.sh

RUN chmod +x /demo/docker-entrypoint.sh

ENTRYPOINT ["/demo/docker-entrypoint.sh"]
CMD ["sim"]

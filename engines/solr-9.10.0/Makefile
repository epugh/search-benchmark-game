SOLR_VERSION := 9.10.0
SOLR_DIR := target/solr-$(SOLR_VERSION)
SOLR_TGZ := solr-$(SOLR_VERSION).tgz
SOLR_CACHE_DIR := $(shell pwd)/../../tmp
SOLR_CACHED_TGZ := $(SOLR_CACHE_DIR)/$(SOLR_TGZ)
SOLR_DOWNLOAD_URL := https://archive.apache.org/dist/solr/solr/$(SOLR_VERSION)/$(SOLR_TGZ)
COLLECTION_NAME := benchmark
SOLR_PORT := 8904
SOLR_HOME := $(shell pwd)/solr_home

clean:
	@echo "--- Cleaning Solr 9.10.0 ---" >&2
	@if [ -d "$(SOLR_DIR)" ] && [ -f "$(SOLR_DIR)/bin/solr" ]; then \
		$(SOLR_DIR)/bin/solr stop -all 2>/dev/null || true; \
	fi
	@rm -fr target
	@rm -fr solr_home
	@rm -fr build

# Cache directory for downloaded files
$(SOLR_CACHE_DIR):
	@mkdir -p $(SOLR_CACHE_DIR)

# Download Solr to cache if not already there
$(SOLR_CACHED_TGZ): $(SOLR_CACHE_DIR)
	@if [ ! -f "$(SOLR_CACHED_TGZ)" ]; then \
		echo "--- Downloading Solr $(SOLR_VERSION) to cache ---" >&2; \
		curl -L -o $(SOLR_CACHED_TGZ) $(SOLR_DOWNLOAD_URL); \
	else \
		echo "--- Using cached Solr $(SOLR_VERSION) ---" >&2; \
	fi

# Extract Solr from cache
$(SOLR_DIR): $(SOLR_CACHED_TGZ)
	@echo "--- Extracting Solr $(SOLR_VERSION) ---" >&2
	@mkdir -p target
	@tar xzf $(SOLR_CACHED_TGZ) -C target

compile:
	@echo "--- Building Solr 9.10.0 ---" >&2
	@./gradlew clean build copyDependencies

start-solr: $(SOLR_DIR)
	@echo "--- Starting Solr ---" >&2
	@if $(SOLR_DIR)/bin/solr status 2>/dev/null | grep -q "running on port $(SOLR_PORT)"; then \
		echo "Solr already running on port $(SOLR_PORT)" >&2; \
	else \
		$(SOLR_DIR)/bin/solr start -c -m 1g -p $(SOLR_PORT) >&2; \
		sleep 5; \
	fi

create-collection: start-solr
	@echo "--- Creating collection $(COLLECTION_NAME) ---" >&2
	@if $(SOLR_DIR)/bin/solr healthcheck -c $(COLLECTION_NAME) --solr-url http://localhost:$(SOLR_PORT)/solr 2>/dev/null | grep -q "healthy"; then \
		echo "Collection $(COLLECTION_NAME) already exists" >&2; \
	else \
		$(SOLR_DIR)/bin/solr create -c $(COLLECTION_NAME) -n _default --solr-url http://localhost:$(SOLR_PORT)/solr >&2; \
		sleep 2; \
	fi

index: compile create-collection
	@echo "--- Indexing Solr 9.10.0 ---" >&2
	@java -server -cp "build/libs/search-index-benchmark-game-solr-1.0-SNAPSHOT.jar:build/dependencies/*" BuildIndex http://localhost:$(SOLR_PORT)/solr $(COLLECTION_NAME) < $(CORPUS)
	@echo "--- Committing index ---" >&2
	@curl -s "http://localhost:$(SOLR_PORT)/solr/$(COLLECTION_NAME)/update?commit=true" > /dev/null

serve: start-solr
	@java -XX:+UseParallelGC -cp "build/libs/search-index-benchmark-game-solr-1.0-SNAPSHOT.jar:build/dependencies/*" DoQuery http://localhost:$(SOLR_PORT)/solr $(COLLECTION_NAME)

stop-solr:
	@echo "--- Stopping Solr ---" >&2
	@if [ -d "$(SOLR_DIR)" ] && [ -f "$(SOLR_DIR)/bin/solr" ]; then \
		$(SOLR_DIR)/bin/solr stop -all >&2; \
	fi

clean-cache:
	@echo "--- Cleaning Solr download cache ---" >&2
	@rm -f $(SOLR_CACHED_TGZ)

.PHONY: clean compile start-solr create-collection index serve stop-solr clean-cache

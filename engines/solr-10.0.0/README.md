# Apache Solr 10.0.0 Search Engine Benchmark

This engine benchmarks Apache Solr 10.0.0 using the SolrJ client library.

## Configuration

- **Solr Version**: 10.0.0 (Release Candidate 3)
- **Download Source**: Apache development distribution (RC3)
- **Port**: 8905 (to avoid conflicts with other Solr engines)
- **Collection**: benchmark
- **Heap Memory**: 1GB (-m 1g)
- **Mode**: Cloud mode (SolrCloud)
- **Similarity**: BM25 (Solr's default)
- **Analyzer**: StandardAnalyzer (text_general field type)
- **Client**: Jetty HTTP/2 SolrJ client (HttpJettySolrClient)
- **Caching**: Enabled (as per production configuration)

## Features

- Multi-threaded indexing with batch size of 1000 documents
- Single shard, single replica configuration for optimal query performance
- Documents indexed with:
  - `text` field: indexed but not stored (for searching)
  - `id` field: stored but not indexed (for identification)
- Query and document caches enabled for realistic performance testing
- Supports full Lucene query parser syntax via `defType=lucene`
- Jetty HTTP/2 client for improved performance (new in Solr 10.0.0)
- Includes HTTP overhead and JSON serialization/deserialization in timing

## Solr 10.0.0 Changes

This engine incorporates the major SolrJ API changes in Solr 10.0.0:

- **Client Migration**: Uses `HttpJettySolrClient` instead of `Http2SolrClient`
- **Package Changes**: HTTP client classes moved to `org.apache.solr.solrj.jetty` package
- **Dependencies**: Includes `solr-solrj-jetty` artifact for Jetty-based HTTP client
- **Query API**: Uses `ModifiableSolrParams` instead of `SolrQuery` for parameter handling
- **Java Requirements**: Minimum Java 21 (up from Java 17 in Solr 9.x)

## Usage

### Build and compile
```bash
make compile
```

### Index documents
```bash
make index CORPUS=/path/to/corpus.json
```

### Start query server
```bash
make serve
```

### Clean up
```bash
make clean
```

## Implementation Details

- **BuildIndex.java**: Multi-threaded document indexing with batching using new `HttpJettySolrClient`
- **DoQuery.java**: Query execution using `ModifiableSolrParams` supporting various search operations
- Uses SolrJ 10.0.0 client library with `solr-solrj-jetty` for HTTP transport
- Supports COUNT, TOP_N, and TOP_N_COUNT query types
- Error handling and progress reporting during indexing
- Automatic collection creation with default schema

## Dependencies

- **solr-solrj**: 10.0.0 - Core SolrJ client library
- **solr-solrj-jetty**: 10.0.0 - Jetty-based HTTP transport layer
- **minimal-json**: 0.9.5 - Lightweight JSON parsing
- **slf4j-simple**: 2.0.9 - Simple logging implementation

## Note

This engine uses Apache Solr 10.0.0 Release Candidate 3, which is not yet officially released. The distribution and Maven artifacts are downloaded from the Apache development repository. The engine has been updated to use the new SolrJ API that was significantly refactored in Solr 10.0.0, including the migration from Apache HttpClient to Eclipse Jetty HTTP client.
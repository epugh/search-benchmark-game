import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

public class BuildIndex {

    private static final int BATCH_SIZE = 1000;
    private static final int QUEUE_SIZE = 10000;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: BuildIndex <solr-url> <collection-name>");
            System.exit(1);
        }

        final String solrUrl = args[0];
        final String collectionName = args[1];

        System.out.println("Connecting to Solr at: " + solrUrl);
        System.out.println("Collection: " + collectionName);

        try (Http2SolrClient solrClient = new Http2SolrClient.Builder(solrUrl).build();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {

            final BlockingQueue<String> workQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
            final AtomicBoolean done = new AtomicBoolean(false);
            final AtomicInteger indexed = new AtomicInteger(0);
            final AtomicInteger errors = new AtomicInteger(0);

            // Create worker threads
            final int numThreads = Runtime.getRuntime().availableProcessors();
            final Thread[] threads = new Thread[numThreads];

            System.out.println("Starting " + numThreads + " indexing threads");

            for (int i = 0; i < numThreads; i++) {
                threads[i] = new Thread(() -> {
                    List<SolrInputDocument> batch = new ArrayList<>(BATCH_SIZE);

                    while (true) {
                        try {
                            String line = workQueue.poll(100, TimeUnit.MILLISECONDS);
                            
                            if (line == null) {
                                if (done.get()) {
                                    // Flush remaining documents
                                    if (!batch.isEmpty()) {
                                        flushBatch(solrClient, collectionName, batch, indexed);
                                        batch.clear();
                                    }
                                    break;
                                }
                                continue;
                            }

                            line = line.trim();
                            if (line.isEmpty()) {
                                continue;
                            }

                            try {
                                final JsonObject parsedDoc = Json.parse(line).asObject();
                                final String id = parsedDoc.get("id").asString();
                                final String text = parsedDoc.get("text").asString();

                                SolrInputDocument doc = new SolrInputDocument();
                                doc.addField("id", id);
                                doc.addField("text", text);
                                batch.add(doc);

                                if (batch.size() >= BATCH_SIZE) {
                                    flushBatch(solrClient, collectionName, batch, indexed);
                                    batch.clear();
                                }
                            } catch (Exception e) {
                                errors.incrementAndGet();
                                if (errors.get() < 10) {
                                    System.err.println("Error parsing document: " + e.getMessage());
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                });
                threads[i].start();
            }

            // Read input and add to queue
            System.out.println("Reading documents from stdin...");
            String line;
            int readCount = 0;
            while ((line = bufferedReader.readLine()) != null) {
                workQueue.put(line);
                readCount++;
                if (readCount % 100000 == 0) {
                    System.out.println("Read: " + readCount + " documents");
                }
            }

            System.out.println("Finished reading " + readCount + " documents");
            done.set(true);

            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }

            final int totalIndexed = indexed.get();
            final int totalErrors = errors.get();

            System.out.println("Indexing complete!");
            System.out.println("Successfully indexed: " + totalIndexed + " documents");
            if (totalErrors > 0) {
                System.out.println("Errors: " + totalErrors);
            }

            // Final commit
            System.out.println("Committing changes...");
            solrClient.commit(collectionName);
            System.out.println("Done!");
        }
    }

    private static void flushBatch(SolrClient solrClient, String collectionName, 
                                   List<SolrInputDocument> batch, AtomicInteger indexed) {
        try {
            solrClient.add(collectionName, batch);
            int newCount = indexed.addAndGet(batch.size());
            if (newCount % 100000 < batch.size()) {
                System.out.println("Indexed: " + newCount + " documents");
            }
        } catch (SolrServerException | IOException e) {
            System.err.println("Error indexing batch: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
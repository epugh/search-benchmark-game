import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.jetty.HttpJettySolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;

public class DoQuery {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: DoQuery <solr-url> <collection-name>");
            System.exit(1);
        }

        final String solrUrl = args[0];
        final String collectionName = args[1];

        try (HttpJettySolrClient solrClient = new HttpJettySolrClient.Builder(solrUrl).build();
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                final String[] fields = line.trim().split("\t");
                if (fields.length != 2) {
                    System.err.println("Invalid input format: " + line);
                    continue;
                }

                final String command = fields[0];
                final String queryStr = fields[1];

                try {
                    final long count = executeQuery(solrClient, collectionName, command, queryStr);
                    if (count == -1) {
                        System.out.println("UNSUPPORTED");
                    } else {
                        System.out.println(count);
                    }
                    System.out.flush();
                } catch (Exception e) {
                    System.err.println("Error executing query: " + e.getMessage());
                    System.out.println("0");
                    System.out.flush();
                }
            }
        }
    }

    private static long executeQuery(SolrClient solrClient, String collectionName, 
                                     String command, String queryStr) 
            throws SolrServerException, IOException {
        
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("q", queryStr);
        params.set("defType", "lucene");
        params.set("df", "text");
        
        // Disable caching for fair benchmarking
        // we run Solr with caching, so lets keep it.
        // params.set("cache", "false");

        switch (command) {
            case "COUNT":
            case "UNOPTIMIZED_COUNT":
                params.set("rows", "0");
                QueryResponse countResponse = solrClient.query(collectionName, params);
                return countResponse.getResults().getNumFound();

            case "TOP_10":
                params.set("rows", "10");
                solrClient.query(collectionName, params);
                return 1;

            case "TOP_100":
                params.set("rows", "100");
                solrClient.query(collectionName, params);
                return 1;

            case "TOP_1000":
                params.set("rows", "1000");
                solrClient.query(collectionName, params);
                return 1;

            case "TOP_10_COUNT":
                params.set("rows", "10");
                QueryResponse top10Response = solrClient.query(collectionName, params);
                return top10Response.getResults().getNumFound();

            case "TOP_100_COUNT":
                params.set("rows", "100");
                QueryResponse top100Response = solrClient.query(collectionName, params);
                return top100Response.getResults().getNumFound();

            case "TOP_1000_COUNT":
                params.set("rows", "1000");
                QueryResponse top1000Response = solrClient.query(collectionName, params);
                return top1000Response.getResults().getNumFound();

            default:
                return -1;  // Signal unsupported command
        }
    }
}
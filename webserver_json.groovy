import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class MyHandler implements HttpHandler {

	public void handle(HttpExchange t) throws IOException {
		JSONObject json = new JSONObject();
		String query = t.getRequestURI().toString();
		Map<String, String> map = getQueryMap(query);
		String httpUrl = map.get("/?param1");
		try {
			json.put("myKey", httpUrl);
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Request headers: " + t.getRequestHeaders());
		System.out.println("Request URI:" + t.getRequestURI());
		System.out.println("value: " + httpUrl);
		try {
			json.put("foo", "bar");
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		t.getResponseHeaders().add("Content-type", "application/json");
		t.sendResponseHeaders(200, json.toString().length());

		String title = getTitle(new URL(httpUrl));
		System.out.println("title:: " + title);

		//
		// Graph persistence
		//

		writeLink(httpUrl, title);

		OutputStream os = t.getResponseBody();
		os.write(json.toString().getBytes());
		os.close();
	}

	public void writeLink(String value, String title) {
		GraphDatabaseService graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabase("yurl.db");
		Transaction tx = graphDb.beginTx();
		try {
			Node firstNode = graphDb.createNode();
			firstNode.setProperty("title", title);
			firstNode.setProperty("url", value);
			tx.success();
		} finally {
			tx.finish();
		}
		printAllNodes(title, graphDb);
		graphDb.shutdown();
	}

	public void printAllNodes(String title, GraphDatabaseService graphDb) {
		Iterable<Node> allNodes = GlobalGraphOperations.at(graphDb).getAllNodes();
		for (final Node node : allNodes) {
			if (node.hasProperty("title")) {
				title = (String) node.getProperty("title");
			}
			String name = "";
			if (node.hasProperty("url")) {
				name = (String) node.getProperty("url");
			}

			System.out.println("\"" + title + "\",\"" + name + "\"");
			System.out.println(name);
			System.out.println();
		}
	}

	public String getTitle(final URL url) {
		String title = "";
		ExecutorService service = Executors.newFixedThreadPool(2);
		Collection<Callable<String>> tasks = new ArrayList<Callable<String>>();
		Callable<String> callable = new Callable<String>() {

			@Override
			public String call() throws Exception {
				Document doc = Jsoup.connect(url.toString()).get();
				return doc.title();

			}
		};
		tasks.add(callable);
		try {
			List<Future<String>> taskFutures = service
					.invokeAll(tasks, 3000L, TimeUnit.SECONDS);
			title = taskFutures.get(0).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return title;
	}

	private Map<String, String> getQueryMap(String query) {
		// Pattern pattern = Pattern.compile("/\\?.*");
		// Matcher matcher = pattern.matcher(query);
		String[] params = query.split("&");
		Map<String, String> map = new HashMap<String, String>();
		for (String param : params) {
			String name = param.split("=")[0];
			String value = param.split("=")[1];
			map.put(name, value);
			System.out.println(name + " :: " + value);
		}
		return map;
	}
}
HttpServer server = HttpServer.create(new InetSocketAddress(4444), 0);
server.createContext("/", new MyHandler());
server.setExecutor(null); // creates a default executor
server.start();
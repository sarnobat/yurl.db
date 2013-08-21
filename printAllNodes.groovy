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


public void printAllNodes() {
	GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("yurl.db");
	Iterable<Node> allNodes = GlobalGraphOperations.at(graphDb).getAllNodes();
	for (final Node node : allNodes) {
		String title = "";
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
	graphDb.shutdown();
}


printAllNodes();
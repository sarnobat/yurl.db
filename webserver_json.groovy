import org.neo4j.graphdb.*;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONObject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import org.apache.commons.io.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


 
class MyHandler implements HttpHandler {
	public Map<String, String> getQueryMap(String query)  
	{  
		Pattern pattern = Pattern.compile("/\\?.*");

		Matcher matcher = pattern.matcher(query);
		String[] params = query.split("&");  
		Map<String, String> map = new HashMap<String, String>();  
		for (String param : params)  
		{  
			String name = param.split("=")[0];  
			String value = param.split("=")[1];  
			map.put(name, value);  
			println(name + " :: " + value);
		}  
		return map;  
	}  

	public void handle(HttpExchange t) throws IOException {
		JSONObject json = new JSONObject();
		String query = t.getRequestURI();
		Map<String, String> map = getQueryMap(query);  
		String value = map.get("/?param1");
		json.put("myKey",value);
		println('Request headers: ' + t.getRequestHeaders());
		println('Request URI:' + t.getRequestURI());
		println('value: ' + value);
		json.put("foo","bar");
		t.getResponseHeaders().add("Access-Control-Allow-Origin","*");
		t.getResponseHeaders().add("Content-type", "application/json");
		t.sendResponseHeaders(200, json.toString().length());
		
		println('1');
		//
		// Graph persistence
		//
		
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("yurl.db");
		
		
		println('2');
		//
		// Create node
		//
		
		Transaction tx = graphDb.beginTx();
		
		println('2.1');
		try
		{
			Node firstNode = graphDb.createNode();
			println('2.2 - ' + value);
			firstNode.setProperty( "url", value);			
			println('2.3');
			// Updating operations go here
			
			println('2.4');
			tx.success();
			
			println('3');
		}
		finally
		{
			tx.finish();
		}
		
		
		//
		// Print nodes
		//
		
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
		OutputStream os = t.getResponseBody();
		os.write(json.toString().getBytes());
		os.close();

	}
}
    
HttpServer server = HttpServer.create(new InetSocketAddress(4444), 0);
server.createContext("/", new MyHandler());
server.setExecutor(null); // creates a default executor
server.start();

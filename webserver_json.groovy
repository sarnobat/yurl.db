
import java.io.DataInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
	
	public String getTitle( URL theURL )
	{
		String startTag = "<title>";
		String endTag = "</title>";
		int startTagLength = startTag.length();
		
		BufferedReader bufReader;
		String line;
		boolean foundStartTag = false;
		boolean foundEndTag = false;
		int startIndex, endIndex;
		String title = "";
		
		try
		{
		  //open file
		  bufReader = new BufferedReader( new InputStreamReader(theURL.openStream()) );
		  
		  //read line by line
		  while( (line = bufReader.readLine()) != null && !foundEndTag)
		  {
			//System.out.println(line);
		  
			//search for title start tag (convert to lower case before searhing)
			if( !foundStartTag && (startIndex = line.toLowerCase().indexOf(startTag)) != -1 )
			{
			  foundStartTag = true;
			}
			else
			{
			  //else copy from start of string
			  startIndex = -startTagLength;
			}
			
			//search for title start tag (convert to lower case before searhing)
			if( foundStartTag && (endIndex = line.toLowerCase().indexOf(endTag)) != -1 )
			{
			  foundEndTag = true;
			}
			else
			{
			  //else copy to end of string
			  endIndex = line.length();
			}
			
			//extract title field
			if( foundStartTag || foundEndTag )
			{
			  //System.out.println("foundStartTag="+foundStartTag);
			  //System.out.println("foundEndTag="+foundEndTag);
			  //System.out.println("startIndex="+startIndex);
			  //System.out.println("startTagLength="+startTagLength);
			  //System.out.println("endIndex="+endIndex);
			
			  title += line.substring( startIndex + startTagLength, endIndex );
			}
		  }
		  
		  //close the file when finished
		  bufReader.close();
		  
		  //output the title
		  if( title.length() > 0 )
		  {
			System.out.println("Title: "+title);
		  }
		  else
		  {
			System.out.println("No title found in document.");
		  }
		  return title;
		  
		}
		catch( IOException e )
		{
		  System.out.println( "GetTitle.GetTitle - error opening or reading URL: " + e );
		}
		return "";
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
		
		
		//
		// Get page title
		//
		println("1");

		URL url = new URL(value);
		String title = getTitle(url);
		println('title:: ' + title);
		
		//
		// Graph persistence
		//
		
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase("yurl.db");
		
		
		//
		// Create node
		//
		
		Transaction tx = graphDb.beginTx();
		
		try
		{
			Node firstNode = graphDb.createNode();
			firstNode.setProperty( "title", title);	
			firstNode.setProperty( "url", value);			
			tx.success();
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

		OutputStream os = t.getResponseBody();
		os.write(json.toString().getBytes());
		os.close();

	}
}
    
HttpServer server = HttpServer.create(new InetSocketAddress(4444), 0);
server.createContext("/", new MyHandler());
server.setExecutor(null); // creates a default executor
server.start();

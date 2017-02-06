package ray.chen.tasks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

public class AggregateTask implements Runnable {
	
	public final static String RESOURCE = "https://gist.githubusercontent.com/"
			+ "jed204/92f90060d0fabf65792d6d479c45f31c/raw/"
			+ "346c44a23762749ede009623db37f4263e94ef2a/java2.json";
	
	// indexes for recursive function that reads json
	private int SENT = 0;
	private int RECV = 1;
	
	public void run() {
		// set current thread name with something meaningful
		final String origName = Thread.currentThread().getName();
        Thread.currentThread().setName("AggregationTask_" + origName);
		
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(RESOURCE);
		
		request.addHeader("Accept", "application/json");
		String respBody = "";
		
		// initiate GET request
		try {
			HttpResponse response = client.execute(request);
			StatusLine statusLine = response.getStatusLine();
			if(statusLine.getStatusCode() != HttpStatus.SC_OK){
				System.out.println("Unsuccessful http call, status" + statusLine.getStatusCode());
				System.out.println("Reason for failure: " + statusLine.getReasonPhrase());
				return;
			}
			
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));

			StringBuilder sb = new StringBuilder();
			String line = "";
			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
			
			respBody = sb.toString();
		} catch (ClientProtocolException cpe){
			System.out.println("ClientProtocolException occured.");
			cpe.printStackTrace();
			return;
		} catch (IOException ioe){
			System.out.println("IOException occured.");
			ioe.printStackTrace();
			return;
		} catch (Exception e){
        	System.out.println("General exception occurred.");
        	e.printStackTrace();
        	return;
        }
		
		// process JSON result
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(respBody);
			
			// statistic map to store field values
			// value: an array containing sent(0) and recv(1) values
			HashMap<String, int[]> stats = new HashMap<>();
			
			// generate a UUID to use as rootName
			String rootName = UUID.randomUUID().toString();
			stats.put(rootName, new int[]{0,0});
			
			readValues(root, rootName, stats);
			
			// print values
			String msg = "Current values: \n";
			
			for(Map.Entry<String, int[]> entry : stats.entrySet()){
				String key = entry.getKey();
				int[] val = entry.getValue();
				key = key.equals(rootName)? "Overall" : key;
				
				msg +=  "-- fieldName : " + key + "\n"
						+ "---- sent: " + val[SENT] + "\n"
						+ "---- recv: " + val[RECV] + "\n";
				
			}
			
			System.out.println(msg);
			
			
		} catch (JsonProcessingException jpe) {
			System.out.println("JsonProcessing Exception occured.");
			jpe.printStackTrace();
		} catch (IOException ioe){
			System.out.println("IOException occured.");
			ioe.printStackTrace();
		} catch (Exception e){
        	System.out.println("General exception occurred.");
        	e.printStackTrace();
        }
		
		
	}
	
	// recursively read from json object and update map entries
	private int[] readValues(JsonNode root, String rootName, HashMap<String, int[]> map){
		if(root == null)
			return new int[]{0,0};
		
		if(root.has("recv") || root.has("sent")){
	    	// we're at the base level
	    	// assume if sent or received is omitted it means no value
	    	int recv = root.has("recv")? root.get("recv").asInt() : 0;
    		int sent = root.has("sent")? root.get("sent").asInt() : 0;
    		
    		int[] arr;
	    	if(map.containsKey(rootName)){
	    		arr = map.get(rootName);
	    		arr[SENT] += sent;
	    		arr[RECV] += recv;
	    		map.put(rootName, arr);
	    		
	    		return arr;
	    	} else {	    		
	    		arr = new int[]{ sent, recv };
	    		map.put(rootName, arr);
	    	}
	    	
	    	return arr;
	    }
		
		// if not at base level, drill into nodes and update stats
		int[] arr = map.containsKey(rootName)? map.get(rootName) : new int[]{0,0};
		Iterator<Entry<String, JsonNode>> it = root.fields();
		while (it.hasNext()){
			Entry<String, JsonNode> entry = it.next();
			
		    String fieldName = entry.getKey();
		    JsonNode fieldVal = entry.getValue();
		    if(fieldVal == null)
		    	continue;
		    
		    int[] nodeStats = readValues(fieldVal, fieldName, map);
		    arr[SENT] += nodeStats[SENT];
		    arr[RECV] += nodeStats[RECV];
		    
		}
		
		map.put(rootName, arr);
		
		return arr;
	}

}

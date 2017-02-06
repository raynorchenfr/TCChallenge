package ray.chen.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.apache.commons.codec.digest.DigestUtils;

public class ChecksumTaskDispatcher {
	
	public final static String RESOURCE = "https://gist.githubusercontent.com/"
			+ "anonymous/8f60e8f49158efdd2e8fed6fa97373a4/raw/"
			+ "01add7ea44ed12f5d90180dc1367915af331492e/java-data2.json";

	private ExecutorService executor;
	private static ChecksumTaskDispatcher instance;
	
	public static Object lock = new Object();
	
	// assign a thread pool for a number of threads
	// this dispatcher is singleton
	private ChecksumTaskDispatcher(int num){
		this.executor = Executors.newFixedThreadPool(num);
	}
	
	
	public static ChecksumTaskDispatcher getInstance(int num){
		
		if(instance == null) {
			synchronized(lock){
				if(instance == null){
					// arbitrary max limit: 3 threads at max
					num = num >= 3? 3:num;
					instance = new ChecksumTaskDispatcher(num);
				}
			}
		}
		
		return instance;
	}
	
	// default thread number: 2
	public static ChecksumTaskDispatcher getInstance(){
		
		if(instance == null) {
			synchronized(lock){
				if(instance == null){
					instance = new ChecksumTaskDispatcher(2);
				}
			}
		}
		
		return instance;
	}
	
	// read data and dispatch the task to a number of tasks
	public void dispatch() {		
		
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
		}
		
		
		// process JSON result and dispatch threads
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(respBody);
			
			if(root.has("items")){
				ArrayNode items = (ArrayNode)root.get("items");
				if(items != null && items.size() != 0){
					int itemCount = 0;
					for(int i=0;i<items.size();i++){
						JsonNode item = items.get(i);
						if(item.has("uid") && item.get("uid") != null){
							String uid = item.get("uid").asText();
							Runnable task = new ChecksumSubTask(uid);
							executor.execute(task);
							
						}
					}
				}
			}
			
		} catch (JsonProcessingException jpe) {
			System.out.println("JsonProcessing Exception occured.");
			jpe.printStackTrace();
		} catch (IOException ioe){
			System.out.println("IOException occured.");
			ioe.printStackTrace();
		} catch (Exception e){
        	System.out.println("General exception occurred.");
        	e.printStackTrace();
        } finally {
        	//executor.shutdown();
        }

	}
	
	
	private class ChecksumSubTask implements Runnable {

		private final String uid;
		
		private ChecksumSubTask(String uid){
			this.uid = uid;
		}
		
		@Override
		public void run() {
			// set current thread name with something meaningful
			final String origName = Thread.currentThread().getName();
			if(!origName.startsWith("ChecksumSubTask_"))
	        	Thread.currentThread().setName("ChecksumSubTask_" + origName);
	        
	        try {
	        	String checkSum = DigestUtils.md5Hex(uid);
	        	System.out.println("Current thread: " + Thread.currentThread().getName()
	        			+ " and MD5 checksum: " + checkSum);
	        	
	        } catch (Exception e){
	        	System.out.println("General exception occurred.");
	        	e.printStackTrace();
	        }
		}
		
	}

}

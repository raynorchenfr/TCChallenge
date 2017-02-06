package ray.chen.main;

import ray.chen.tasks.AggregateTask;
import ray.chen.tasks.ChecksumTaskDispatcher;

public class ChallengeRunner {

	public static void main(String[] args) throws InterruptedException {
		
		// no arguments, run both
		if(args == null || args.length == 0){
			runAggregate();
			
			Thread.sleep(3000);
			runChecksum();
		} else if(args[0].equals("1") || args[0].equals("aggregate")){
			runAggregate();
		} else if(args[0].equals("2") || args[0].equals("checksum")){
			runChecksum();
		}
	}
	
	private static void runAggregate(){
		AggregateTask aTask = new AggregateTask();
		aTask.run();
	}
	
	private static void runChecksum(){
		ChecksumTaskDispatcher checksum = ChecksumTaskDispatcher.getInstance(3);
		checksum.dispatch();
	}

}

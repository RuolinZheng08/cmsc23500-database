package uchi.introdb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author aelmore
 * Do not modify this class.
 */
public class Driver {
	private int capLines = -1;
	private int batchSize = 1;
	private List<Predicate<?>> predicates1 = null;
	private List<Predicate<?>> predicates2 = null;
	public static String INPUT_FILE= "311_Service_Requests_-_Pot_Holes_Reported.tsv";

	public Driver(int batchSize, int capLines) {
		this.capLines = capLines;
		this.batchSize = batchSize;
		predicates1 = new ArrayList<>();
		predicates2 = new ArrayList<>();
		buildPredicates(predicates1, predicates2);
	}

	public List<String> runBenchmark(DatabaseWrapper dbWrapper, List<String> lines, boolean useIndex){
		List<String> res = new ArrayList<>();
		try{
			long start, end;
			if (!dbWrapper.openConnection()){
				System.out.println("Error opening connection. Halting program");
				System.exit(-2);
			}
			dbWrapper.dropPotholeTable();
			dbWrapper.createPotholeTable();
			if (useIndex){
				dbWrapper.createPotholeIndexes();
			}
			start = System.currentTimeMillis();
			for (String line : lines){
				dbWrapper.loadPotholeRecord(line);
			}
			dbWrapper.finalizeLoading();
			end  = System.currentTimeMillis();
			long load = end -start;
			res.add(String.format("Loading,index=%s,lines=%s,time=%s", useIndex,lines.size(), load));

			start = System.currentTimeMillis();
			for (int i = 0; i < predicates1.size(); i++){
				dbWrapper.getServiceRequestNumbers(predicates1.get(i), predicates2.get(i));
			}
			end  = System.currentTimeMillis();
			long query = end -start;
			res.add(String.format("Querying,index=%s,queries=%s,time=%s", useIndex,predicates1.size(), query));

			List<String> servs = dbWrapper.getServiceRequestNumbers(new Predicate<String>("WM CDOT Recommended Restoration Transfer Outcome", Predicate.Compare.EQUALS, Predicate.Field.RECENT_ACTIONS), null);
			if (servs==null) {
				res.add("Did not get expected number of service requests=null");
			}
			else if (servs.size()!= 78) {
				res.add("Did not get expected number of service requests="+servs.size());
			}
		} catch (Exception ex){
			ex.printStackTrace();
		} finally{
			dbWrapper.closeConnection();
		}
		return res;


	}

	public void run() {
		FileReader fr = null;
		BufferedReader br = null;
		DatabaseWrapper dbWrapper = null;
		try{
			fr = new FileReader(INPUT_FILE);
			br = new BufferedReader(fr);
			dbWrapper = new DatabaseWrapper(batchSize);
			System.out.println("TSV data:");
			String line = br.readLine();
			System.out.println(line);
			int count = 0;
			line = br.readLine();
			List<String> lines = new ArrayList<>();
			while(line != null){
				if (capLines > 0 && count++ > capLines-1)
					break;
				lines.add(line);
				line = br.readLine();

			}
			List<String> res = new ArrayList<>();
			res.addAll(runBenchmark(dbWrapper,lines, false));
			res.addAll(runBenchmark(dbWrapper,lines, true));
			System.out.println("Output to include in writeup:\n====================================================================");
			for (String s: res) {
				System.out.println(s);
			}

		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex){
			ex.printStackTrace();
		}
		finally {
			try{
				if (fr != null){
					fr.close();
				}
				if (br != null) br.close();
			} catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	private final String[] RECENT_ACTIONS = {
	 "Completed", //                                                                    1028
	 "CDOT Street Cut Complaints Transfer Outcome",//                                   965
	 "CDOT Inspect Public Way Survey Transfer Outcome",//                               739
	 "No Such Address Found",//                                                          455
	 "No Problem Found",//                                                               300
	 "No Action - See Remarks in Description",//                                         297
	 "Not Within CDOT Jurisdiction",//                                                   232
	 "CDOT Pavement Buckle or Speed Hump Transfer Outcome",//                            174
	 "No Jurisdiction",//                                                                145
	 "Private Property - Owner's Responsibility",//                                       90
	 "CDOT Alley Grading - Unimproved Transfer Outcome",//                                84
	 "WM CDOT Recommended Restoration Transfer Outcome",//                                67
	 "Street Under Construction - Transfer to Inspect Public Way Construction",//         60
	 "WM Water Management General Investigation Transfer Outcome"};//                      59

	private final  int[] ZIPS = {
			60604,//      578
			60602,//      543
			60666,//       51
			60635,//       22
			60627,//        4
	};

	//LAT 41.772910    	41.960925
	// LONG 	-87.756321  -87.56
	private final double LATMAX = 41.960925;
	private final double LATMIN = 41.648461;

	private final double LONGMAX = 41.960925;
	private final double LONGMIN = 41.648461;


	public void buildPredicates(List<Predicate<?>> p1, List<Predicate<?>> p2) {
		Random generator = new Random();
		//ZIPs
		for (int i = 0 ; i < 250; i ++ ) {
			p1.add(new Predicate<Integer>(ZIPS[generator.nextInt(ZIPS.length)], Predicate.Compare.EQUALS, Predicate.Field.ZIP));
			p2.add(null);
		}
		//ZIPs and Potholes
		for (int i = 0 ; i < 450; i ++ ) {
			p1.add(new Predicate<Integer>(ZIPS[generator.nextInt(ZIPS.length)], Predicate.Compare.EQUALS, Predicate.Field.ZIP));
			p2.add(new Predicate<Integer>(generator.nextInt(30), Predicate.Compare.GREATERTHAN, Predicate.Field.NUM_POTHOLES));
		}
		//Lats
		for (int i = 0 ; i < 2000; i ++ ) {
			p1.add(new Predicate<Double>(-1*LATMIN+generator.nextDouble()* (LATMAX-LATMIN), Predicate.Compare.GREATERTHAN, Predicate.Field.LONGITUDE));
			p2.add(new Predicate<Double>(-1*LATMIN+generator.nextDouble()* (LATMAX-LATMIN), Predicate.Compare.LESSTHAN, Predicate.Field.LONGITUDE));
		}
		//Lats
		for (int i = 0 ; i < 130; i ++ ) {
			p1.add(new Predicate<Double>(-1*LONGMIN+generator.nextDouble()* (LONGMAX-LONGMIN), Predicate.Compare.GREATERTHAN, Predicate.Field.LONGITUDE));
			p2.add(new Predicate<Double>(-1*LONGMIN+generator.nextDouble()* (LONGMAX-LONGMIN), Predicate.Compare.LESSTHAN, Predicate.Field.LONGITUDE));
		}

		//ZIPs
		for (int i = 0 ; i < 50; i ++ ) {
			p1.add(new Predicate<Integer>(ZIPS[generator.nextInt(ZIPS.length)], Predicate.Compare.EQUALS, Predicate.Field.ZIP));
			p2.add(null);
		}

		//ZIPs
		for (int i = 0 ; i < 40; i ++ ) {
			p1.add(new Predicate<String>(RECENT_ACTIONS[generator.nextInt(RECENT_ACTIONS.length)], Predicate.Compare.EQUALS, Predicate.Field.RECENT_ACTIONS));
			p2.add(null);
		}

	}

	public static void main(String[] args) {
		int limitRows = -1;
		int batchSize = 1;
		String id = null;
		if (args.length >= 1 ){
			try{
				if (args[0].length() > 0)
					batchSize = Integer.parseInt(args[0]);
				if (args.length >= 2) {
					if (args[1].length() > 0) 
						limitRows = Integer.parseInt(args[1]);
				}
				if (args.length>=3) {
					id = args[2];
				}
			} catch (Exception ex){
				System.err.println("Exception trying to set batch size or limitRows. \n Usage: Driver [batchSize] [limitRows]\n batch size is optional, and  limitRows is optional if batchSize is given.");
				ex.printStackTrace();
				System.exit(-1);
			}

		}
		//Note change argument to limit Driver lines for testing only
		String limitString  =  (limitRows > -1 ) ? " Limiting loading records to : "+limitRows : " Loading all records" ;
		//String idString  = (id == null) ? "": " cnetID/database name set to :"+id;
		System.out.println("Running HW driver with batchSize: " + batchSize + limitString);
		//DatabaseConstants.DBNAME = id;
		//DatabaseConstants.USERNAME = id;
		
		Driver driver = new Driver(batchSize,limitRows);
		driver.run();
	}

}

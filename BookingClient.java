/* MULTITHREADING BookingClient.java
 * EE422C Project 6 submission by
 * Replace <...> with your actual data.
 * Fawadul Haq
 * fh5277
 * 16225
 * Slip days used: 0
 * Spring 2019
 */
package assignment6;

import java.util.Map;

import assignment6.Theater.BoxOffice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.lang.Thread;

public class BookingClient {

	private Map<String, Integer> office;
	private Theater theater;
	
    /**
     * @param office  maps box office id to number of customers in line
     * @param theater the theater where the show is playing
     */
    public BookingClient(Map<String, Integer> office, Theater theater) {
        this.office = office;
        this.theater = theater;
    }

    /**
     * Starts the box office simulation by creating (and starting) threads
     * for each box office to sell tickets for the given theater
     *
     * @return list of threads used in the simulation,
     * should have as many threads as there are box offices
     */
    public List<Thread> simulate() {
        List<Thread> threads = new ArrayList<Thread>();
        
        // Iterate through the Box Offices and create threads for them
        for(String ID : office.keySet()) {
        	BoxOffice newBO = theater.new BoxOffice(ID, office.get(ID));
        	Thread BOthread = new Thread(newBO);
        	threads.add(BOthread);
        }
        // Start them all
        for(Thread thread : threads) {
        	thread.start();
        }
        
        return threads;
    }

    public static void main(String[] args) {
    	// Test Initialization /////////////////////////////
        Map<String, Integer> test_office = new HashMap<String, Integer>(); 
        test_office.put("BX1", 5); 
        test_office.put("BX3", 5); 
        test_office.put("BX2", 5);
        test_office.put("BX5", 5); 
        test_office.put("BX4", 5);
        Theater test_theater = new Theater(30, 20, "Ouija");
        BookingClient bc = new BookingClient(test_office, test_theater);
        ////////////////////////////////////////////////////
        List<Thread> BoxOffices = bc.simulate();
       
        
        // Join them
        for(Thread t : BoxOffices) {
        	try {
        		t.join();
        	} catch (InterruptedException e){
        		throw new RuntimeException(e);
        	}
        }
    
        System.out.println("\nPrinting Log\n" + test_theater.getTransactionLog());
        
    }
}

/* MULTITHREADING Theater.java
 * EE422C Project 6 submission by
 * Replace <...> with your actual data.
 * Fawadul Haq
 * fh5277
 * 16225
 * Slip days used: 0
 * Spring 2019
 */
package assignment6;

import java.util.*;
import java.util.Collections.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Theater {

	private int numRows;
	private int seatsPerRow;
	private String show;
	private Seat nextBestSeat;
	private int numTicketsPrinted;
	private AtomicInteger currClient;
	private List<Ticket> Log;
    
    /**
     * Compares tickets for the order they should have been purchased (A1 > A2 > B1> ...)
     */
    private Comparator<Ticket> compTickets = new Comparator<Ticket>() {
    	@Override
    	public int compare(Ticket T1, Ticket T2) {
    		int retval = T1.getSeat().toString().compareTo(T2.getSeat().toString());
    		return retval;
    	}
    };
    
	
    /**
     * the delay time you will use when print tickets
     */
    private int printDelay = 500; // Use it in your Thread.sleep()

    public void setPrintDelay(int printDelay) {
        this.printDelay = printDelay;
    }

    public int getPrintDelay() {
        return printDelay;
    }
            ////////////////////////////////////
///////////////////// NESTED CLASSES /////////////////////////////
            ////////////////////////////////////
    /**
     * Represents a seat in the theater
     * A1, A2, A3, ... B1, B2, B3 ...
     */
    static class Seat {
        private int rowNum;  // zero-indexed
        private int seatNum; // one-indexed

        public Seat(int rowNum, int seatNum) {
            this.rowNum = rowNum; 
            this.seatNum = seatNum;
        }

        public int getSeatNum() {
            return seatNum;
        }

        public int getRowNum() {
            return rowNum;
        }

        @Override
        public String toString() {
            String result = "";
            int tempRowNumber = rowNum + 1;
            do {
                tempRowNumber--;
                result = ((char) ('A' + tempRowNumber % 26)) + result;
                tempRowNumber = tempRowNumber / 26;
            } while (tempRowNumber > 0);
            result += seatNum;
            return result;
        }
        
        @Override
        public boolean equals(Object seat) {
        	
        	if(rowNum == ((Seat)seat).getRowNum())
        		if(seatNum == ((Seat)seat).getSeatNum())
        			return true;
        	
        	return false;
        }
    }

    /**
     * Represents a ticket purchased by a client
     */
    static class Ticket {
        private String show;
        private String boxOfficeId;
        private Seat seat;
        private int client;
        public static final int ticketStringRowLength = 31;


        public Ticket(String show, String boxOfficeId, Seat seat, int client) {
            this.show = show;
            this.boxOfficeId = boxOfficeId;
            this.seat = seat;
            this.client = client;
        }

        public Seat getSeat() {
            return seat;
        }

        public String getShow() {
            return show;
        }

        public String getBoxOfficeId() {
            return boxOfficeId;
        }

        public int getClient() {
            return client;
        }

        @Override
        public String toString() {
            String result, dashLine, showLine, boxLine, seatLine, clientLine, eol;

            eol = System.getProperty("line.separator");

            dashLine = new String(new char[ticketStringRowLength]).replace('\0', '-');

            showLine = "| Show: " + show;
            for (int i = showLine.length(); i < ticketStringRowLength - 1; ++i) {
                showLine += " ";
            }
            showLine += "|";

            boxLine = "| Box Office ID: " + boxOfficeId;
            for (int i = boxLine.length(); i < ticketStringRowLength - 1; ++i) {
                boxLine += " ";
            }
            boxLine += "|";

            seatLine = "| Seat: " + seat.toString();
            for (int i = seatLine.length(); i < ticketStringRowLength - 1; ++i) {
                seatLine += " ";
            }
            seatLine += "|";

            clientLine = "| Client: " + client;
            for (int i = clientLine.length(); i < ticketStringRowLength - 1; ++i) {
                clientLine += " ";
            }
            clientLine += "|";

            result = dashLine + eol +
                    showLine + eol +
                    boxLine + eol +
                    seatLine + eol +
                    clientLine + eol +
                    dashLine;

            return result;
        }
    }

    /**
     * Represents a Box Office handling customers and printing tickets
     */
    public class BoxOffice implements Runnable {
    	private String ID;
    	private Integer Ppl_inLine;
		
    	BoxOffice(String ID, Integer Ppl_inLine){
    		this.ID = ID;
    		this.Ppl_inLine = Ppl_inLine;
    	}
    	
    	@Override
		public void run(){
    		
			while(Ppl_inLine > 0) {
				
				Seat bestSeat = bestAvailableSeat();
				if(bestSeat == null) return; // If no seat available, we are sold out
				
				synchronized(currClient) {  // synchronize currClient to ensure no repeats
					
					while(printTicket(ID, bestSeat, currClient.get()) == null) { 
						// Keep checking for bestAvailableSeats
						bestSeat = bestAvailableSeat();
						if(bestSeat == null) return;
					}
					currClient.getAndAdd(1);
				}
				
				Ppl_inLine--;
			}
			
		}
    
    } 

              //////////////////////////////////////
///////////////////// END NESTED CLASSES /////////////////////////////
              /////////////////////////////////////
    
    // Theater Constructor
    public Theater(int numRows, int seatsPerRow, String show) {
    	this.numRows = numRows;
    	this.seatsPerRow = seatsPerRow;
    	this.show = show;
    	nextBestSeat = new Seat(0,1); // Initialize row to zero so that firstBestSeat is one after it
    	numTicketsPrinted = 0;
    	currClient = new AtomicInteger(1);
    	Log = new ArrayList<Ticket>(); // Holds the unordered list of tickets purchased
    }

    /**
     * Calculates the best seat not yet reserved
     *
     * @return the best seat or null if theater is full
     */
    public synchronized Seat bestAvailableSeat() {
    	
    	// If the last seat reserved was the last in the theater
    	if(isSoldOut()) {
    		return null;
    	}
    	
        return nextBestSeat;
    }

    /**
     * Prints a ticket for the client after they reserve a seat
     * Also prints the ticket to the console
     *
     * @param seat a particular seat in the theater
     * @return a ticket or null if a box office failed to reserve the seat
     * @throws InterruptedException 
     */
    public synchronized Ticket printTicket(String boxOfficeId, Seat seat, int client) {
        // Null check
    	if(seat == null) {  
        	return null;
        }
        // Checking if seat has been taken
        if(!Log.isEmpty()) { 
        	Seat lastSeatPrinted = Log.get(Log.size()-1).getSeat(); 
        	//If the latest seat is GREATER than the seat attempting to print, then you need to redo
        	if(lastSeatPrinted.getRowNum() > seat.getRowNum())
        		return null;
        	if(lastSeatPrinted.getRowNum() == seat.getRowNum())
        		if(lastSeatPrinted.getSeatNum() >= seat.getSeatNum())
        			return null;
        }
        		
        Ticket newTicket = new Ticket(show, boxOfficeId, seat, client);
        
        ///// Calculate next Best Seat
    	int nextRow = nextBestSeat.getRowNum();
    	int nextSeatNum = nextBestSeat.getSeatNum();
    	if(nextBestSeat.getSeatNum() == seatsPerRow) { // if we're at the end of row
    		nextRow++;
    		nextSeatNum = 1; // reset seat number
    	} else {
    		nextSeatNum++;
    	}
    
    	nextBestSeat = new Seat(nextRow, nextSeatNum); // reset nextBestSeat
    	///// Done Calculating
    	
    	
        // Managing files
    	System.out.println(newTicket.toString());
    	numTicketsPrinted++;
    	Log.add(newTicket);
    	
    	// If we printed the last available ticket just now
    	if(numTicketsPrinted >= numRows*seatsPerRow) {
    		System.out.println("Sorry, we are sold out!");
    		return null;
    	}
		
    	try {
			Thread.sleep(printDelay);
		} catch (InterruptedException e) {}
        
        return newTicket;
    }

    /**
     * Lists all tickets sold for this theater in order of purchase
     *
     * @return list of tickets sold
     */
    public List<Ticket> getTransactionLog() {
        return Log;
    }
   
    /**
     * Tells whether the last seat reserved was the last possible seat
     * 
     * @return boolean answering the question
     */
    private synchronized boolean isSoldOut() {
    	// if the next Best Seat's row index spills over into a row that doesn't exist
    	if(nextBestSeat.getRowNum() == numRows) {
    		return true;
    	}
    	
    	return false;
    }

}

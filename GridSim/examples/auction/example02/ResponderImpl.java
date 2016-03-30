package auction.example02;

/*
 * Author: Marcos Dias de Assuncao
 * Date: March 2006
 * Description: A simple program to demonstrate of how to use GridSim
 *              auction extension package.
 *              This example shows how to create user, resource, auctioneer
 *              and auction entities connected via a network topology,
 *              using link and router.
 *
 */

import eduni.simjava.distributions.Sim_uniform_obj;
import gridsim.AllocPolicy;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;
import gridsim.auction.AuctionTags;
import gridsim.auction.Message;
import gridsim.auction.MessageBid;
import gridsim.auction.MessageCallForBids;
import gridsim.auction.MessageInformOutcome;
import gridsim.auction.MessageInformStart;
import gridsim.auction.MessageRejectBid;
import gridsim.auction.MessageRejectCallForBid;
import gridsim.auction.Responder;

import java.util.Hashtable;

/**
 * This class implements a responder for an auction.
 * A responder corresponds to the bidder's side of the auction.
 * When messages such as call for bids, reject proposals and so on are
 * sent to the bidder, the bidder forwards the messages to its observer,
 * which in turn uses a responder to get a reply message.
 *
 * @author Marcos Dias de Assuncao
 *
 */
public class ResponderImpl implements Responder {
	// this distribution is used to generate an
	// internal price for the resource
	private ResourceCharacteristics resource;
	private Hashtable pAuctions;
	private Sim_uniform_obj genBid;


	/**
	 * Constructor (parameters are used just to ilustrate that the responder could
	 * take into account the usage of the resource or a give policy to formulate a bid
	 * @param resource
	 * @param calendar
	 * @param policy
	 * @throws Exception
	 */
	public ResponderImpl(ResourceCharacteristics resource,
					ResourceCalendar calendar,
					AllocPolicy policy) throws Exception{
		this.resource = resource;
		pAuctions = new Hashtable();
		long seed = 11L*13*17*19*23+1;
		genBid = new Sim_uniform_obj("gen_bid",0,10,seed);
	}

	/**
	 * This method is invoked when a call for bids is received
	 * @param msg the call for bids received
	 * @return the reply message to the call for bids
	 * @see gridsim.auction.Responder#onReceiveCfb(MessageCallForBids)
	 */
	public Message onReceiveCfb(MessageCallForBids msg){
		int auctionID = msg.getAuctionID();
		int prot = msg.getProtocol();
		int round = msg.getRound();

		Message response = null;

		ParticipatingAuction pAuction = (ParticipatingAuction)pAuctions.get(new Integer(auctionID));
		pAuction.setProtocol(prot);

		double currPrice = msg.getPrice();
		double priceBid = 0;

		// sets the responder with the parameters for this round
		pAuction.setCurrentPrice(currPrice);
		pAuction.setLastCfpID(msg.getMessageID());

		// we defined cost as random up to 10 to guarantee that there will be bids
		// for executing the job
		double jobCost = genBid.sample();
		pAuction.setJobCost(jobCost);

		boolean shouldBid = jobCost < currPrice;

		// Treats the call for proposal according to the corresponding auction protocol
		if(prot == AuctionTags.REVERSE_FIRST_PRICE_SEALED_AUCTION){
			if(shouldBid){

				response = new MessageBid(auctionID,prot,
						msg.getMessageID(),0,round);

				priceBid = (float)jobCost;
				((MessageBid)response).setPrice(priceBid);

				System.out.println(resource.getResourceName() + " bidding for auction " +
						auctionID + " round " + round + " and price " + priceBid);
			}
			else{
				response = new MessageRejectCallForBid(auctionID, prot,
						msg.getMessageID(), 0,round);
			}
		}
		else if(prot == AuctionTags.REVERSE_DUTCH_AUCTION){
			if(shouldBid){
				response = new MessageBid(auctionID,prot,
						msg.getMessageID(),0,round);

				priceBid = currPrice;
				((MessageBid)response).setPrice(priceBid);

				System.out.println(resource.getResourceName() + " bidding for auction " +
						auctionID + " round " + round + " and price " + priceBid);
			}
			else{
				response = new MessageRejectCallForBid(auctionID, prot,
						msg.getMessageID(), 0,round);
			}
		}
		else if(prot == AuctionTags.REVERSE_ENGLISH_AUCTION){
			if(shouldBid){
				response = new MessageBid(auctionID,prot,
						msg.getMessageID(),0,round);

				priceBid = currPrice;
				((MessageBid)response).setPrice(priceBid);

				System.out.println(resource.getResourceName() + " bidding for auction " +
						auctionID + " round " + round + " and price " + priceBid);
			}
			else{
				response = new MessageRejectCallForBid(auctionID, prot,
						msg.getMessageID(), 0, round);
			}
		}

		pAuction.setLastOffer(priceBid);
		return response;
	}

	 /**
	 * This method is invoked when an inform outcome message is received
	 * @param msg the inform outcome received
	 * @return the reply message to the inform outcome
	 * @see gridsim.auction.Responder#onReceiveInformOutcome(gridsim.auction.MessageInformOutcome)
	 */
	public Message onReceiveInformOutcome(MessageInformOutcome msg){
		int auctionID = msg.getAuctionID();
		int winner = msg.getWinnerID();

		ParticipatingAuction pAuction = (ParticipatingAuction)pAuctions.get(new Integer(auctionID));

		if(winner == resource.getResourceID()){
			double finalPrice = msg.getPrice();
			pAuction.setCurrentPrice(finalPrice);
		}

		pAuction = (ParticipatingAuction)pAuctions.get(new Integer(auctionID));
		pAuctions.remove(new Integer(auctionID));
		return null;
	}

	 /**
	 * This method is invoked when a reject for proposal is received
	 * @param msg the reject proposal received
	 * @return the reply message to the reject proposal
	 * @see gridsim.auction.Responder#onReceiveRejectProposal(MessageRejectBid)
	 */
	public Message onReceiveRejectProposal(MessageRejectBid msg){
//		int auctionID = msg.getAuctionID();
//		ParticipatingAuction pAuction = (ParticipatingAuction)pAuctions.get(new Integer(auctionID));
		return null;
	}

	 /**
	 * This method is invoked when an auction starts and an
	 * inform start message is received
	 * @param inform start received
	 * @return the reply message to the inform start
	 * @see gridsim.auction.Responder#onReceiveStartAuction(MessageInformStart)
	 */
	public Message onReceiveStartAuction(MessageInformStart inform){
		int auctionID = inform.getAuctionID();

		// creates the responder to deal with the auction and puts it into the pAuctions
		ParticipatingAuction pAuction = new ParticipatingAuction(auctionID);
		pAuction.setProtocol(inform.getProtocol());
		pAuctions.put(new Integer(auctionID),pAuction);

		return null;
	}

    /**
     * This class is just for the bidder to keep track of the auctions in
     * which it is involved
     *
     * @author Marcos Dias de Assunção
     */
    class ParticipatingAuction {
    	int auctionID;
    	int lastCfpID;
    	double lastOffer;
    	double currentPrice;
    	int protocol;
    	double jobCost;

    	/**
    	 * Constructor
    	 * @param auction the auction id
    	 */
    	public ParticipatingAuction(int auction){
    		this.auctionID = auction;
    	}

    	/**
    	 * Returns the auction id
    	 * @return the auction id
    	 */
    	public int getAuctionID(){
    		return auctionID;
    	}

    	/**
    	 * Sets the price of the last offer made
    	 * @param price the last price
    	 */
    	public void setLastOffer(double price){
    		synchronized(this){
    			this.lastOffer = price;
    		}
    	}

    	/**
    	 * Returns the value of the last offer made
    	 * @return the last offer
    	 */
    	public double getLastOffer(){
    		synchronized(this){
    			return this.lastOffer;
    		}
    	}

    	/**
    	 * Sets the current price of the auction
    	 * @param price the current price
    	 */
    	public void setCurrentPrice(double price){
    		this.currentPrice = price;
    	}

    	/**
    	 * Returns the current price of the auction
    	 * @return the current price
    	 */
    	public double getCurrentPrice(){
    		return currentPrice;
    	}


    	/**
    	 * Sets the id of the last Call for Bids received
    	 * @param cfpID last call for bid's id
    	 */
    	public void setLastCfpID(int cfpID){
    		lastCfpID = cfpID;
    	}

    	/**
    	 * Returns the id of the last call for bids received
    	 * @return the last call for bids
    	 */
    	public int getLastCfpID(){
    		return lastCfpID;
    	}

    	/**
    	 * Sets the auction protocol used by the auction
    	 * @param protocol the id of the auction protocol
    	 */
    	public void setProtocol(int protocol){
    		this.protocol = protocol;
    	}

    	/**
    	 * Returns the auction protocol's id used in this auction
    	 * @return the protocol's id
    	 */
    	public int getProtocol(){
    		return protocol;
    	}

    	/**
    	 * Sets the cost for executing the job, which the user
    	 * wants to submit
    	 * @param cost the cost of executing the job
    	 */
    	public void setJobCost(double cost){
    		this.jobCost = cost;
    	}

    	/**
    	 * Returns the cost of executing the job, which the user
    	 * wants to submit
    	 * @return the cost of executing the job
    	 */
    	public double getJobCost(){
    		return this.jobCost;
    	}
    }
}
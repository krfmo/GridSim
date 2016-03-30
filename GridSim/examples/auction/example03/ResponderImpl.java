package auction.example03;

/*
 * Author: Marcos Dias de Assuncao
 * Date: May 2008
 * Description: A simple program to demonstrate of how to use GridSim
 *              auction extension package.
 *              This example shows how to create user, resource, auctioneer
 *              and auction entities connected via a network topology,
 *              using link and router.
 *
 */

import gridsim.AllocPolicy;
import gridsim.ResourceCalendar;
import gridsim.ResourceCharacteristics;
import gridsim.auction.Message;
import gridsim.auction.MessageCallForBids;
import gridsim.auction.MessageInformOutcome;
import gridsim.auction.MessageInformStart;
import gridsim.auction.MessageRejectBid;
import gridsim.auction.Responder;

/**
 * This class implements a responder for an auction.
 *
 * @author Marcos Dias de Assuncao
 *
 */
public class ResponderImpl implements Responder {
	// this distribution is used to generate an
	// internal price for the resource
	private ResourceCharacteristics resource;

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
	}

	/**
	 * This method is invoked when a call for bids is received
	 * @param msg the call for bids received
	 * @return the reply message to the call for bids
	 * @see gridsim.auction.Responder#onReceiveCfb(MessageCallForBids)
	 */
	public Message onReceiveCfb(MessageCallForBids msg) {
		return null;
	}

	 /**
	 * This method is invoked when an inform outcome message is received
	 * @param msg the inform outcome received
	 * @return the reply message to the inform outcome
	 * @see gridsim.auction.Responder#onReceiveInformOutcome(gridsim.auction.MessageInformOutcome)
	 */
	public Message onReceiveInformOutcome(MessageInformOutcome msg){
		return null;
	}

	 /**
	 * This method is invoked when a reject for proposal is received
	 * @param msg the reject proposal received
	 * @return the reply message to the reject proposal
	 * @see gridsim.auction.Responder#onReceiveRejectProposal(MessageRejectBid)
	 */
	public Message onReceiveRejectProposal(MessageRejectBid msg){
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
		return null;
	}
}
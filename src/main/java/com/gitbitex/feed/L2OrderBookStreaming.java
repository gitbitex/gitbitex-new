package com.gitbitex.feed;

import com.gitbitex.feed.message.L2UpdateMessage;
import com.gitbitex.matchingengine.snapshot.L2OrderBookUpdate;
import com.gitbitex.matchingengine.snapshot.OrderBookManager;

import java.util.concurrent.BlockingQueue;

public class L2OrderBookStreaming extends Thread {
    private OrderBookManager orderBookManager;
    private BlockingQueue<L2OrderBookUpdate.Change> changeQueue;

    public void sendChange(L2OrderBookUpdate.Change change){

    }

    public void run(){
        while (!Thread.currentThread().isInterrupted()){
            try {
                // send snapshot

                L2OrderBookUpdate.Change change= changeQueue.take();

                //

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}

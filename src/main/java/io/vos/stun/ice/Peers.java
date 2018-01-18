package io.vos.stun.ice;

/**
 * Created by wuh56 on 4/25/2017.
 */
public class Peers {
    private Peers(){};

    /** Lengths of message header parts in bytes */
    public static final int NODE_STATUS_INIT = 0;
    public static final int NODE_STATUS_WAITING_REQUEST = 1;
    public static final int NODE_STATUS_WAITING_RESPONSE = 2;
    public static final int NODE_STATUS_NEGOCIATED = 3;
    public static final int NODE_STATUS_TIMEOUT = 4;

}

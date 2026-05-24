package com.subastas.event;

import com.subastas.model.dto.websocket.BidConfirmedMessage;
import com.subastas.model.dto.websocket.BidUpdatedMessage;
import org.springframework.context.ApplicationEvent;

public class PujaConfirmadaEvent extends ApplicationEvent {

    private final Long subastaId;
    private final String emailPostor;
    private final BidUpdatedMessage bidUpdated;
    private final BidConfirmedMessage bidConfirmed;

    public PujaConfirmadaEvent(Object source, Long subastaId, String emailPostor,
                               BidUpdatedMessage bidUpdated, BidConfirmedMessage bidConfirmed) {
        super(source);
        this.subastaId    = subastaId;
        this.emailPostor  = emailPostor;
        this.bidUpdated   = bidUpdated;
        this.bidConfirmed = bidConfirmed;
    }

    public Long getSubastaId()               { return subastaId; }
    public String getEmailPostor()            { return emailPostor; }
    public BidUpdatedMessage getBidUpdated()  { return bidUpdated; }
    public BidConfirmedMessage getBidConfirmed() { return bidConfirmed; }
}

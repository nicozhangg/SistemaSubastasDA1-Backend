package com.subastas.event;

import org.springframework.context.ApplicationEvent;

public class SubastaCerradaItemEvent extends ApplicationEvent {

    private final String emailGanador;
    private final String nombreGanador;
    private final String descripcionItem;
    private final String desglose;

    public SubastaCerradaItemEvent(Object source, String emailGanador, String nombreGanador,
                                   String descripcionItem, String desglose) {
        super(source);
        this.emailGanador = emailGanador;
        this.nombreGanador = nombreGanador;
        this.descripcionItem = descripcionItem;
        this.desglose = desglose;
    }

    public String getEmailGanador()     { return emailGanador; }
    public String getNombreGanador()    { return nombreGanador; }
    public String getDescripcionItem()  { return descripcionItem; }
    public String getDesglose()         { return desglose; }
}

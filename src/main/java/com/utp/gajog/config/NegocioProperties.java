package com.utp.gajog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gajog.negocio")
public class NegocioProperties {

    private int diasAvisoPorVencer = 5;
    private int diasGraciaMora = 15;
    private boolean bajaAutomaticaMora = false;
    private int asistenciaMinimaExamen = 75;
    private int mesesMinimosExamen = 3;
    private int minutosToleranciaTardanza = 10;

    public int getDiasAvisoPorVencer() {
        return diasAvisoPorVencer;
    }

    public void setDiasAvisoPorVencer(int diasAvisoPorVencer) {
        this.diasAvisoPorVencer = diasAvisoPorVencer;
    }

    public int getDiasGraciaMora() {
        return diasGraciaMora;
    }

    public void setDiasGraciaMora(int diasGraciaMora) {
        this.diasGraciaMora = diasGraciaMora;
    }

    public boolean isBajaAutomaticaMora() {
        return bajaAutomaticaMora;
    }

    public void setBajaAutomaticaMora(boolean bajaAutomaticaMora) {
        this.bajaAutomaticaMora = bajaAutomaticaMora;
    }

    public int getAsistenciaMinimaExamen() {
        return asistenciaMinimaExamen;
    }

    public void setAsistenciaMinimaExamen(int asistenciaMinimaExamen) {
        this.asistenciaMinimaExamen = asistenciaMinimaExamen;
    }

    public int getMesesMinimosExamen() {
        return mesesMinimosExamen;
    }

    public void setMesesMinimosExamen(int mesesMinimosExamen) {
        this.mesesMinimosExamen = mesesMinimosExamen;
    }

    public int getMinutosToleranciaTardanza() {
        return minutosToleranciaTardanza;
    }

    public void setMinutosToleranciaTardanza(int minutosToleranciaTardanza) {
        this.minutosToleranciaTardanza = minutosToleranciaTardanza;
    }
}
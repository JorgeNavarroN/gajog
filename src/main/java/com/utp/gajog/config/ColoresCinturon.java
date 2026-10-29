package com.utp.gajog.config;

import org.springframework.stereotype.Component;


@Component("coloresCinturon")
public class ColoresCinturon {

    public String fondo(String color) {
        return switch (normalizar(color)) {
            case "BLANCO" ->
                "#f4f7fc";
            case "AMARILLO" ->
                "#f2cf3a";
            case "NARANJA" ->
                "#f08a24";
            case "VERDE" ->
                "#2fa866";
            case "AZUL" ->
                "#2f6fe0";
            case "ROJO" ->
                "#d6283f";
            case "NEGRO" ->
                "#05070a";
            default ->
                "#3a475a";
        };
    }

    public String texto(String color) {
        return switch (normalizar(color)) {
            case "BLANCO", "AMARILLO" ->
                "#11171f";
            default ->
                "#ffffff";
        };
    }

    private String normalizar(String color) {
        return color == null ? "" : color.trim().toUpperCase();
    }
}

package com.andregattermeyer.api_frota.dto.request;

import com.andregattermeyer.api_frota.entity.Tipo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VeiculoRequestDTO {

    @NotBlank(message = "Placa é obrigatória")
    @Size(min = 7, max = 8, message = "Placa inválida")
    private String placa;

    @NotBlank(message = "Modelo é obrigatório")
    private String modelo;

    @NotBlank(message = "Marca é obrigatória")
    private String marca;

    private String cor;
    private String frota;
    private String renavan;

    @NotNull(message = "Tipo é obrigatório")
    private Tipo tipo;

    // Getters e Setters
    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public String getFrota() {
        return frota;
    }

    public void setFrota(String frota) {
        this.frota = frota;
    }

    public String getRenavan() {
        return renavan;
    }

    public void setRenavan(String renavan) {
        this.renavan = renavan;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }
}
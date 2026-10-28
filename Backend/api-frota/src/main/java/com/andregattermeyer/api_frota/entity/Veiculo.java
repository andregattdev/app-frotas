package com.andregattermeyer.api_frota.entity;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_veiculo")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String placa;
    private String modelo;
    private String marca;
    private String cor;
    private String frota;
    private String renavan;

    /** 
     * Relacionamento com a entidade Motorista
     * 
    */
    @ManyToMany
    @JoinTable(name = "veiculo_motorista", joinColumns = @JoinColumn(name = "veiculo_id"), inverseJoinColumns = @JoinColumn(name = "motorista_id"))
    @JsonIgnoreProperties("veiculos")
    private List<Motorista> motoristas = new ArrayList<>();

    /** 
     * Relacionamento com a entidade Checklist
     * 
    */
    @OneToMany(mappedBy = "veiculo")
    @JsonIgnoreProperties("veiculo")
    private List<Checklist> checklists = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "veiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PontoParada> pontosParada = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Tipo tipo;

   
    public Veiculo() {
    }

    public Veiculo(Long id, String placa, String modelo, String marca, String cor, String frota, String renavan,
            List<Checklist> checklists, List<Motorista> motoristas, Tipo tipo) {
        this.id = id;
        this.placa = placa;
        this.modelo = modelo;
        this.marca = marca;
        this.cor = cor;
        this.frota = frota;
        this.renavan = renavan;
        this.checklists = checklists;
        this.motoristas = motoristas;
        this.tipo = tipo;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<Motorista> getMotoristas() {
        return motoristas;
    }

    public void setMotoristas(List<Motorista> motoristas) {
        this.motoristas = motoristas;
    }

    public List<Checklist> getChecklists() {
        return checklists;
    }

    public void setChecklists(List<Checklist> checklists) {
        this.checklists = checklists;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }
}
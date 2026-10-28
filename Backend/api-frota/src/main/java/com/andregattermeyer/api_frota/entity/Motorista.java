package com.andregattermeyer.api_frota.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_motorista")
public class Motorista {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true, nullable = false)
    private String cnh;

    private String categoria;

    private LocalDate validadeCnh;
    
    private String telefone;

    @ManyToMany(mappedBy = "motoristas")
    @JsonIgnoreProperties("motoristas")
    private List<Veiculo> veiculos = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "endereco_id")
    @JsonIgnoreProperties("motorista")
    private Endereco endereco;

    @OneToMany(mappedBy = "motorista")
    @JsonIgnoreProperties("motorista")
    private List<Checklist> checklists = new ArrayList<>();

    
    public Motorista() {
    }


    public Motorista(Long id, String nome, String cnh, String categoria, LocalDate validadeCnh, String telefone, List<Veiculo> veiculos, Endereco endereco, List<Checklist> checklists) {
        this.id = id;
        this.nome = nome;
        this.cnh = cnh;
        this.categoria = categoria;
        this.validadeCnh = validadeCnh;
        this.telefone = telefone;
        this.veiculos = veiculos;
        this.endereco = endereco;
        this.checklists = checklists;
    }
    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCnh() {
        return cnh;
    }

    public void setCnh(String cnh) {
        this.cnh = cnh;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public LocalDate getValidadeCnh() {
        return validadeCnh;
    }

    public void setValidadeCnh(LocalDate validadeCnh) {
        this.validadeCnh = validadeCnh;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public List<Veiculo> getVeiculos() {
        return veiculos;
    }

    public void setVeiculos(List<Veiculo> veiculos) {
        this.veiculos = veiculos;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }
}
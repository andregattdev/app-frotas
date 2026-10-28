package com.andregattermeyer.api_frota.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_oficina")
public class Oficina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeOficina;  
    private String telefone;  
    private String especialidade; // ex: mecânica geral, elétrica, pneus

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "endereco_id", referencedColumnName = "id")
    private Endereco endereco;


    public Oficina() {
    }


    public Oficina(Long id, String nomeOficina, String telefone, String especialidade, Endereco endereco) {
        this.id = id;
        this.nomeOficina = nomeOficina;
        this.telefone = telefone;
        this.especialidade = especialidade;
        this.endereco = endereco;
    }

   

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNomeOficina() {
        return this.nomeOficina;
    }

    public void setNomeOficina(String nomeOficina) {
        this.nomeOficina = nomeOficina;
    }

    public String getTelefone() {
        return this.telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEspecialidade() {
        return this.especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public Endereco getEndereco() {
        return this.endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }




}

package com.andregattermeyer.api_frota.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_ponto_parada")
public class PontoParada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Column(length = 255)
    private String nomeEstabelecimento;

    @Column(length = 500)
    private String endereco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoParada tipo;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer ordem;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rota_diaria_id")
    private RotaDiaria rotaDiaria;

    public PontoParada() {
    }

    public PontoParada(Long id, String descricao, TipoParada tipo, Double latitude, Double longitude) {
        this.id = id;
        this.descricao = descricao;
        this.tipo = tipo;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public PontoParada(Long id, String descricao, TipoParada tipo, Double latitude, Double longitude,
                       Integer ordem, Veiculo veiculo) {
        this(id, descricao, tipo, latitude, longitude);
        this.ordem = ordem;
        this.veiculo = veiculo;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return this.descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getNomeEstabelecimento() {
        return this.nomeEstabelecimento;
    }

    public void setNomeEstabelecimento(String nomeEstabelecimento) {
        this.nomeEstabelecimento = nomeEstabelecimento;
    }

    public String getEndereco() {
        return this.endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public TipoParada getTipo() {
        return this.tipo;
    }

    public void setTipo(TipoParada tipo) {
        this.tipo = tipo;
    }

    public Double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return this.longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getOrdem() {
        return this.ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }

    public Veiculo getVeiculo() {
        return this.veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public RotaDiaria getRotaDiaria() {
        return rotaDiaria;
    }

    public void setRotaDiaria(RotaDiaria rotaDiaria) {
        this.rotaDiaria = rotaDiaria;
    }
}

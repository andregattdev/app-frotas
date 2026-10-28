package com.andregattermeyer.api_frota.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.andregattermeyer.api_frota.dto.request.ChecklistRequestDTO;
import com.andregattermeyer.api_frota.dto.request.EnderecoRequestDTO;
import com.andregattermeyer.api_frota.dto.request.MotoristaRequestDTO;
import com.andregattermeyer.api_frota.dto.request.OficinaRequestDTO;
import com.andregattermeyer.api_frota.dto.request.VeiculoRequestDTO;
import com.andregattermeyer.api_frota.dto.response.ChecklistResponseDTO;
import com.andregattermeyer.api_frota.dto.response.EnderecoResponseDTO;
import com.andregattermeyer.api_frota.dto.response.MotoristaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.OficinaResponseDTO;
import com.andregattermeyer.api_frota.dto.response.VeiculoResponseDTO;
import com.andregattermeyer.api_frota.entity.Checklist;
import com.andregattermeyer.api_frota.entity.Endereco;
import com.andregattermeyer.api_frota.entity.Motorista;
import com.andregattermeyer.api_frota.entity.Oficina;
import com.andregattermeyer.api_frota.entity.Veiculo;



public class Mapper {

    // ==================== ENDERECO ====================

    public static Endereco toEnderecoEntity(EnderecoRequestDTO dto) {
        if (dto == null)
            return null;

        Endereco endereco = new Endereco();
        endereco.setLogradouro(dto.getLogradouro());
        endereco.setNumero(dto.getNumero());
        endereco.setComplemento(dto.getComplemento());
        endereco.setBairro(dto.getBairro());
        endereco.setCidade(dto.getCidade());
        endereco.setEstado(dto.getEstado());
        endereco.setCep(dto.getCep());
        return endereco;
    }

    public static EnderecoResponseDTO toEnderecoResponseDTO(Endereco entity) {
        if (entity == null)
            return null;

        EnderecoResponseDTO dto = new EnderecoResponseDTO();
        dto.setId(entity.getId());
        dto.setLogradouro(entity.getLogradouro());
        dto.setNumero(entity.getNumero());
        dto.setComplemento(entity.getComplemento());
        dto.setBairro(entity.getBairro());
        dto.setCidade(entity.getCidade());
        dto.setEstado(entity.getEstado());
        dto.setCep(entity.getCep());
        return dto;
    }

    // ==================== MOTORISTA ====================

    public static Motorista toMotoristaEntity(MotoristaRequestDTO dto) {
        if (dto == null)
            return null;

        Motorista motorista = new Motorista();
        motorista.setNome(dto.getNome());
        motorista.setCnh(dto.getCnh());
        motorista.setCategoria(dto.getCategoria());
        motorista.setValidadeCnh(dto.getValidadeCnh());
        motorista.setTelefone(dto.getTelefone());
        motorista.setEndereco(toEnderecoEntity(dto.getEndereco()));
        return motorista;
    }

    public static MotoristaResponseDTO toMotoristaResponseDTO(Motorista entity) {
        if (entity == null)
            return null;

        MotoristaResponseDTO dto = new MotoristaResponseDTO();
        dto.setId(entity.getId());
        dto.setNome(entity.getNome());
        dto.setCnh(entity.getCnh());
        dto.setCategoria(entity.getCategoria());
        dto.setValidadeCnh(entity.getValidadeCnh());
        dto.setTelefone(entity.getTelefone());
        dto.setEndereco(toEnderecoResponseDTO(entity.getEndereco()));
        return dto;
    }

    public static List<MotoristaResponseDTO> toMotoristaResponseDTOList(List<Motorista> entities) {
        return entities.stream()
                .map(Mapper::toMotoristaResponseDTO)
                .collect(Collectors.toList());
    }

    // ==================== VEICULO ====================

    public static Veiculo toVeiculoEntity(VeiculoRequestDTO dto) {
        if (dto == null)
            return null;

        Veiculo veiculo = new Veiculo();
        veiculo.setPlaca(dto.getPlaca());
        veiculo.setModelo(dto.getModelo());
        veiculo.setMarca(dto.getMarca());
        veiculo.setCor(dto.getCor());
        veiculo.setFrota(dto.getFrota());
        veiculo.setRenavan(dto.getRenavan());
        veiculo.setTipo(dto.getTipo());
        return veiculo;
    }

    public static VeiculoResponseDTO toVeiculoResponseDTO(Veiculo entity) {
        if (entity == null)
            return null;

        VeiculoResponseDTO dto = new VeiculoResponseDTO();
        dto.setId(entity.getId());
        dto.setPlaca(entity.getPlaca());
        dto.setModelo(entity.getModelo());
        dto.setMarca(entity.getMarca());
        dto.setCor(entity.getCor());
        dto.setFrota(entity.getFrota());
        dto.setRenavan(entity.getRenavan());
        dto.setTipo(entity.getTipo());
        return dto;
    }

    public static List<VeiculoResponseDTO> toVeiculoResponseDTOList(List<Veiculo> entities) {
        return entities.stream()
                .map(Mapper::toVeiculoResponseDTO)
                .collect(Collectors.toList());
    }

    // ==================== CHECKLIST ====================

    public static Checklist toChecklistEntity(ChecklistRequestDTO dto) {
        if (dto == null)
            return null;

        Checklist checklist = new Checklist();
        checklist.setDataHora(dto.getDataHora());
        checklist.setQuilometragem(dto.getQuilometragem());
        checklist.setPneus(dto.getPneus());
        checklist.setFarois(dto.getFarois());
        checklist.setLanternas(dto.getLanternas());
        checklist.setFreios(dto.getFreios());
        checklist.setNivelOleo(dto.getNivelOleo());
        checklist.setNivelAgua(dto.getNivelAgua());
        checklist.setDocumentos(dto.getDocumentos());
        checklist.setExtintor(dto.getExtintor());
        checklist.setTriangulo(dto.getTriangulo());
        checklist.setMacaco(dto.getMacaco());
        checklist.setEstepe(dto.getEstepe());
        checklist.setLimpeza(dto.getLimpeza());
        checklist.setObservacoes(dto.getObservacoes());

        // Os objetos Veiculo e Motorista serão setados no Service
        // (precisamos buscar as entities pelo ID)

        return checklist;
    }

    public static ChecklistResponseDTO toChecklistResponseDTO(Checklist entity) {
        if (entity == null)
            return null;

        ChecklistResponseDTO dto = new ChecklistResponseDTO();
        dto.setId(entity.getId());
        dto.setDataHora(entity.getDataHora());
        dto.setQuilometragem(entity.getQuilometragem());
        dto.setPneus(entity.getPneus());
        dto.setFarois(entity.getFarois());
        dto.setLanternas(entity.getLanternas());
        dto.setFreios(entity.getFreios());
        dto.setNivelOleo(entity.getNivelOleo());
        dto.setNivelAgua(entity.getNivelAgua());
        dto.setDocumentos(entity.getDocumentos());
        dto.setExtintor(entity.getExtintor());
        dto.setTriangulo(entity.getTriangulo());
        dto.setMacaco(entity.getMacaco());
        dto.setEstepe(entity.getEstepe());
        dto.setLimpeza(entity.getLimpeza());
        dto.setObservacoes(entity.getObservacoes());
        dto.setVeiculo(toVeiculoResponseDTO(entity.getVeiculo()));
        dto.setMotorista(toMotoristaResponseDTO(entity.getMotorista()));
        return dto;
    }

    public static List<ChecklistResponseDTO> toChecklistResponseDTOList(List<Checklist> entities) {
        return entities.stream()
                .map(Mapper::toChecklistResponseDTO)
                .collect(Collectors.toList());
    }



    // ==================== OFICINA ====================

    public static Oficina toOficinaEntity(OficinaRequestDTO dto) {
        if (dto == null)
            return null;

        Oficina oficina = new Oficina();
        oficina.setNomeOficina(dto.getNomeOficina());
        oficina.setTelefone(dto.getTelefone());
        oficina.setEspecialidade(dto.getEspecialidade());
        oficina.setEndereco(toEnderecoEntity(dto.getEndereco()));
        return oficina;
    }
        
       
    

    public static OficinaResponseDTO toOficinaResponseDTO(Oficina entity) {
        if (entity == null)
            return null;

        OficinaResponseDTO dto = new OficinaResponseDTO();
        dto.setId(entity.getId());
        dto.setNomeOficina(entity.getNomeOficina());
        dto.setEspecialidade(entity.getEspecialidade());
        dto.setTelefone(entity.getTelefone());
        dto.setEndereco(toEnderecoResponseDTO(entity.getEndereco()));
        return dto;
    }

    public static List<OficinaResponseDTO> toOficinaResponseDTOList(List<Oficina> entities) {
        return entities.stream()
                .map(Mapper::toOficinaResponseDTO)
                .collect(Collectors.toList());
    }

}
package com.andregattermeyer.api_frota.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.andregattermeyer.api_frota.dto.request.OficinaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.OficinaResponseDTO;
import com.andregattermeyer.api_frota.entity.Endereco;
import com.andregattermeyer.api_frota.entity.Oficina;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.mapper.Mapper;
import com.andregattermeyer.api_frota.repository.OficinaRepository;
import com.andregattermeyer.api_frota.repository.SearchSpecifications;

import jakarta.persistence.criteria.JoinType;

@Service 
public class OficinaService {

    @Autowired
    private OficinaRepository oficinaRepository;

    public List<OficinaResponseDTO> listarTodas(String busca) {
        if (busca == null || busca.isBlank()) {
            return Mapper.toOficinaResponseDTOList(oficinaRepository.findAll());
        }

        String termo = busca.toLowerCase();
        return Mapper.toOficinaResponseDTOList(oficinaRepository.findAll().stream()
                .filter(oficina -> contem(oficina.getNomeOficina(), termo)
                        || contem(oficina.getTelefone(), termo)
                        || contem(oficina.getEspecialidade(), termo))
                .toList());
    }

    public Page<OficinaResponseDTO> listarPagina(String busca, Pageable pageable) {
        if (busca == null || busca.isBlank()) {
            return oficinaRepository.findAll(pageable).map(Mapper::toOficinaResponseDTO);
        }

        var specification = (org.springframework.data.jpa.domain.Specification<Oficina>) (root, query, builder) -> {
            var endereco = root.join("endereco", JoinType.LEFT);
            return builder.or(
                    SearchSpecifications.containsIgnoreCase(builder, root.get("nomeOficina").as(String.class), busca),
                    SearchSpecifications.containsIgnoreCase(builder, root.get("telefone").as(String.class), busca),
                    SearchSpecifications.containsIgnoreCase(builder, root.get("especialidade").as(String.class), busca),
                    SearchSpecifications.containsIgnoreCase(builder, endereco.get("logradouro").as(String.class), busca),
                    SearchSpecifications.containsIgnoreCase(builder, endereco.get("bairro").as(String.class), busca),
                    SearchSpecifications.containsIgnoreCase(builder, endereco.get("cidade").as(String.class), busca),
                    SearchSpecifications.containsIgnoreCase(builder, endereco.get("estado").as(String.class), busca),
                    SearchSpecifications.containsIgnoreCase(builder, endereco.get("cep").as(String.class), busca));
        };

        return oficinaRepository.findAll(specification, pageable).map(Mapper::toOficinaResponseDTO);
    }

    private boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }

    public OficinaResponseDTO buscarPorId(Long id) {
        Oficina oficina = oficinaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada com o id: " + id));
        return Mapper.toOficinaResponseDTO(oficina);
    }

    public OficinaResponseDTO salvar(OficinaRequestDTO dto) {
        oficinaRepository.findByNomeOficina(dto.getNomeOficina())
                .ifPresent(o -> {
                    throw new BadRequestException("Já existe uma oficina cadastrada com o nome: " + dto.getNomeOficina());
                });

        Oficina oficina = Mapper.toOficinaEntity(dto);
        Oficina salvo = oficinaRepository.save(oficina);
        return Mapper.toOficinaResponseDTO(salvo);
    }

    public OficinaResponseDTO atualizar(Long id, OficinaRequestDTO dto) {
        Oficina oficinaExistente = oficinaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada com o id: " + id));

        oficinaRepository.findByNomeOficina(dto.getNomeOficina())
                .filter(o -> !o.getId().equals(id))
                .ifPresent(o -> {
                    throw new BadRequestException("Já existe uma oficina cadastrada com o nome: " + dto.getNomeOficina());
                });

        oficinaExistente.setNomeOficina(dto.getNomeOficina());
        oficinaExistente.setEspecialidade(dto.getEspecialidade());
        oficinaExistente.setTelefone(dto.getTelefone());

        if (dto.getEndereco() != null) {
            if (oficinaExistente.getEndereco() != null) {
                Endereco end = oficinaExistente.getEndereco();
                end.setLogradouro(dto.getEndereco().getLogradouro());
                end.setNumero(dto.getEndereco().getNumero());
                end.setComplemento(dto.getEndereco().getComplemento());
                end.setBairro(dto.getEndereco().getBairro());
                end.setCidade(dto.getEndereco().getCidade());
                end.setEstado(dto.getEndereco().getEstado());
                end.setCep(dto.getEndereco().getCep());
            } else {
                oficinaExistente.setEndereco(Mapper.toEnderecoEntity(dto.getEndereco()));
            }
        }

        Oficina atualizado = oficinaRepository.save(oficinaExistente);
        return Mapper.toOficinaResponseDTO(atualizado);
    }

    public void deletar(Long id) {
        if (!oficinaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Oficina não encontrada com o id: " + id);
        }
        oficinaRepository.deleteById(id);
    }

    public OficinaResponseDTO buscarPorNomeOficina(String nomeOficina) {
        Oficina oficina = oficinaRepository.findByNomeOficina(nomeOficina)
                .orElseThrow(() -> new ResourceNotFoundException("Oficina não encontrada com o nome: " + nomeOficina));
        return Mapper.toOficinaResponseDTO(oficina);
    }

    public List<OficinaResponseDTO> buscarPorNomeParcial(String termo) {
        return Mapper.toOficinaResponseDTOList(oficinaRepository.findByNomeOficinaContainingIgnoreCase(termo));
    }

    public List<OficinaResponseDTO> buscarPorEspecialidade(String especialidade) {
        return Mapper.toOficinaResponseDTOList(oficinaRepository.findByEspecialidade(especialidade));
    }

    public List<OficinaResponseDTO> procurarPorEndereco(String termo) {
        return Mapper.toOficinaResponseDTOList(oficinaRepository.procurarPorEndereco(termo));
    }
}

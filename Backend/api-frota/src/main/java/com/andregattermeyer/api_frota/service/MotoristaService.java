package com.andregattermeyer.api_frota.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.andregattermeyer.api_frota.dto.request.MotoristaRequestDTO;
import com.andregattermeyer.api_frota.dto.response.MotoristaResponseDTO;
import com.andregattermeyer.api_frota.entity.Motorista;
import com.andregattermeyer.api_frota.exception.BadRequestException;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.mapper.Mapper;
import com.andregattermeyer.api_frota.repository.MotoristaRepository;
import com.andregattermeyer.api_frota.repository.SearchSpecifications;

@Service
public class MotoristaService {

    @Autowired
    private MotoristaRepository motoristaRepository;

    /**
     * Listar todos os motoristas cadastrados no sistema.
     */

    public List<MotoristaResponseDTO> listarTodos(String busca) {
        if (busca == null || busca.isBlank()) {
            return Mapper.toMotoristaResponseDTOList(motoristaRepository.findAll());
        }

        String termo = busca.toLowerCase();
        return Mapper.toMotoristaResponseDTOList(motoristaRepository.findAll().stream()
                .filter(motorista -> contem(motorista.getNome(), termo) || contem(motorista.getCnh(), termo)
                        || contem(motorista.getCategoria(), termo) || contem(motorista.getTelefone(), termo))
                .toList());
    }

    public Page<MotoristaResponseDTO> listarPagina(String busca, Pageable pageable) {
        return motoristaRepository.findAll(
                SearchSpecifications.containsAnyIgnoreCase(busca, "nome", "cnh", "categoria", "telefone"),
                pageable).map(Mapper::toMotoristaResponseDTO);
    }

    private boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }

    /**
     * Listar motorista por ID.
     * @param id
     * @return retorna o motorista procurado pelo seu ID.
     */
    public MotoristaResponseDTO buscarPorId(Long id) {
        Motorista motorista = motoristaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista nao encontrado com o id: " + id));
        return Mapper.toMotoristaResponseDTO(motorista);
    }

    /**
     * Cadastrar motorista no sistema
     */
    public MotoristaResponseDTO salvar(MotoristaRequestDTO dto) {
        motoristaRepository.findByCnh(dto.getCnh())
                .ifPresent(m -> {
                    throw new BadRequestException("Ja existe um motorista cadastrado com a CNH: " + dto.getCnh());
                });

        Motorista motorista = Mapper.toMotoristaEntity(dto);
        Motorista salvo = motoristaRepository.save(motorista);
        return Mapper.toMotoristaResponseDTO(salvo);
    }

    /**
     * Atualizar os dados do motorista cadastrado no sistema
     */
    public MotoristaResponseDTO atualizar(Long id, MotoristaRequestDTO dto) {
        Motorista motoristaExistente = motoristaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista nao encontrado com o id: " + id));

        motoristaRepository.findByCnh(dto.getCnh())
                .filter(m -> !m.getId().equals(id))
                .ifPresent(m -> {
                    throw new BadRequestException("Ja existe um motorista cadastrado com a CNH: " + dto.getCnh());
                });

        motoristaExistente.setNome(dto.getNome());
        motoristaExistente.setCnh(dto.getCnh());
        motoristaExistente.setCategoria(dto.getCategoria());
        motoristaExistente.setValidadeCnh(dto.getValidadeCnh());
        motoristaExistente.setTelefone(dto.getTelefone());

        if (dto.getEndereco() != null) {
            motoristaExistente.setEndereco(Mapper.toEnderecoEntity(dto.getEndereco()));
        }

        Motorista atualizado = motoristaRepository.save(motoristaExistente);
        return Mapper.toMotoristaResponseDTO(atualizado);
    }

    /**
     * Deletar o motorista cadastrado no sistemaa
     */
    public void deletar(Long id) {
        if (!motoristaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Motorista nao encontrado com o id: " + id);
        }
        motoristaRepository.deleteById(id);
    }

    public List<MotoristaResponseDTO> buscarPorNome(String nome) {
        return Mapper.toMotoristaResponseDTOList(motoristaRepository.findByNome(nome));
    }

    public List<MotoristaResponseDTO> buscarPorNomeIgnoreCase(String nome) {
        return Mapper.toMotoristaResponseDTOList(motoristaRepository.findByNomeIgnoreCase(nome));
    }

    public List<MotoristaResponseDTO> buscarPorNomeParcial(String nome) {
        return Mapper.toMotoristaResponseDTOList(motoristaRepository.findByNomeContainingIgnoreCase(nome));
    }

    public MotoristaResponseDTO buscarPorCnh(String cnh) {
        Motorista motorista = motoristaRepository.findByCnh(cnh)
                .orElseThrow(() -> new ResourceNotFoundException("Motorista nao encontrado com a CNH: " + cnh));
        return Mapper.toMotoristaResponseDTO(motorista);
    }

    public List<MotoristaResponseDTO> buscarPorCategoria(String categoria) {
        return Mapper.toMotoristaResponseDTOList(motoristaRepository.findByCategoria(categoria));
    }

    public List<MotoristaResponseDTO> buscarMotoristasComCnhVencida() {
        return Mapper.toMotoristaResponseDTOList(motoristaRepository.findMotoristasComCnhVencida());
    }
}

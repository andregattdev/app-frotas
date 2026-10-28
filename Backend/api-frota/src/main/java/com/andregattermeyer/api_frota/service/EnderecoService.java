package com.andregattermeyer.api_frota.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.andregattermeyer.api_frota.dto.request.EnderecoRequestDTO;
import com.andregattermeyer.api_frota.dto.response.EnderecoResponseDTO;
import com.andregattermeyer.api_frota.entity.Endereco;
import com.andregattermeyer.api_frota.exception.ResourceNotFoundException;
import com.andregattermeyer.api_frota.mapper.Mapper;
import com.andregattermeyer.api_frota.repository.EnderecoRepository;
import com.andregattermeyer.api_frota.repository.SearchSpecifications;

@Service
public class EnderecoService {

    @Autowired
    private EnderecoRepository enderecoRepository;

    public List<EnderecoResponseDTO> listarTodos(String busca) {
        String termo = busca == null ? "" : busca.toLowerCase();
        return enderecoRepository.findAll().stream()
                .filter(endereco -> termo.isBlank() || contem(endereco.getLogradouro(), termo) || contem(endereco.getNumero(), termo)
                        || contem(endereco.getBairro(), termo) || contem(endereco.getCidade(), termo)
                        || contem(endereco.getEstado(), termo) || contem(endereco.getCep(), termo))
                .map(Mapper::toEnderecoResponseDTO)
                .collect(Collectors.toList());
    }

    public Page<EnderecoResponseDTO> listarPagina(String busca, Pageable pageable) {
        return enderecoRepository.findAll(
                SearchSpecifications.containsAnyIgnoreCase(
                        busca, "logradouro", "numero", "bairro", "cidade", "estado", "cep"),
                pageable).map(Mapper::toEnderecoResponseDTO);
    }

    private boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }

    public EnderecoResponseDTO buscarPorId(Long id) {
        Endereco endereco = enderecoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com o id: " + id));
        return Mapper.toEnderecoResponseDTO(endereco);
    }

    public EnderecoResponseDTO salvar(EnderecoRequestDTO dto) {
        Endereco endereco = Mapper.toEnderecoEntity(dto);
        Endereco salvo = enderecoRepository.save(endereco);
        return Mapper.toEnderecoResponseDTO(salvo);
    }

    public EnderecoResponseDTO atualizar(Long id, EnderecoRequestDTO dto) {
        Endereco enderecoExistente = enderecoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com o id: " + id));

        enderecoExistente.setLogradouro(dto.getLogradouro());
        enderecoExistente.setNumero(dto.getNumero());
        enderecoExistente.setComplemento(dto.getComplemento());
        enderecoExistente.setBairro(dto.getBairro());
        enderecoExistente.setCidade(dto.getCidade());
        enderecoExistente.setEstado(dto.getEstado());
        enderecoExistente.setCep(dto.getCep());

        Endereco atualizado = enderecoRepository.save(enderecoExistente);
        return Mapper.toEnderecoResponseDTO(atualizado);
    }

    public void deletar(Long id) {
        if (!enderecoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Endereço não encontrado com o id: " + id);
        }
        enderecoRepository.deleteById(id);
    }

    public EnderecoResponseDTO buscarPorCep(String cep) {
        Endereco endereco = enderecoRepository.findByCep(cep)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado com o CEP: " + cep));
        return Mapper.toEnderecoResponseDTO(endereco);
    }

    public List<EnderecoResponseDTO> buscarPorCidade(String cidade) {
        return enderecoRepository.findByCidade(cidade).stream()
                .map(Mapper::toEnderecoResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EnderecoResponseDTO> buscarPorEstado(String estado) {
        return enderecoRepository.findByEstado(estado).stream()
                .map(Mapper::toEnderecoResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EnderecoResponseDTO> buscarPorBairro(String bairro) {
        return enderecoRepository.findByBairro(bairro).stream()
                .map(Mapper::toEnderecoResponseDTO)
                .collect(Collectors.toList());
    }
}

package com.andregattermeyer.api_frota.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.andregattermeyer.api_frota.dto.response.ViaCepResponseDTO;

@Service
public class ViaCepService {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://viacep.com.br")
            .build();

    public ViaCepResponseDTO consultar(String cep) {
        String cepNormalizado = cep.replaceAll("\\D", "");

        return restClient.get()
                .uri("/ws/{cep}/json/", cepNormalizado)
                .retrieve()
                .body(ViaCepResponseDTO.class);
    }
}
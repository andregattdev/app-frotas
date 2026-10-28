package com.andregattermeyer.api_frota.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String mensagem) {
        super(mensagem);
    }

}

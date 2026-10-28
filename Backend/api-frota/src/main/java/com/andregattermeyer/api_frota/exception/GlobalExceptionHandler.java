package com.andregattermeyer.api_frota.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestPart(
            MissingServletRequestPartException ex, WebRequest request) {
        String mensagem = "Envie os dados do registro e uma foto JPEG ou PNG.";
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                mensagem,
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "Payload Too Large",
                "A foto deve ter no máximo 8 MB.",
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    // Erros de validação do @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Erro de validação nos campos",
                request.getDescription(false).replace("uri=", ""),
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailure(AuthenticationException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Usuário ou senha inválidos.",
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Você não tem acesso a este recurso.",
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erro inesperado: " + ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
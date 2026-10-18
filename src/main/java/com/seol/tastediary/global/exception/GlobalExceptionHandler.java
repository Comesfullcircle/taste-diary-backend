package com.seol.tastediary.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateEmail(DuplicateEmailException exception) {
        return new ErrorResponse("DUPLICATE_EMAIL", exception.getMessage(), List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthentication() {
        return new ErrorResponse("INVALID_CREDENTIALS", "이메일 또는 비밀번호를 확인해 주세요.", List.of());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleParameterValidation() {
        return new ErrorResponse("INVALID_INPUT",
                "페이지는 0 이상, 페이지 크기는 1~100이어야 합니다.", List.of());
    }

    @ExceptionHandler(EntryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntryNotFoundException exception) {
        return new ErrorResponse("ENTRY_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(
                        switch (error.getField()) {
                            case "ratingStepValid" -> "rating";
                            case "passwordLengthValid" -> "password";
                            default -> error.getField();
                        },
                        error.getDefaultMessage()))
                .toList();
        return new ErrorResponse("INVALID_INPUT", "입력값을 확인해 주세요.", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody() {
        return new ErrorResponse("INVALID_REQUEST", "JSON 형식과 필드 값의 타입을 확인해 주세요.", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch() {
        return new ErrorResponse("INVALID_REQUEST", "경로 또는 요청 파라미터의 타입을 확인해 주세요.", List.of());
    }
}

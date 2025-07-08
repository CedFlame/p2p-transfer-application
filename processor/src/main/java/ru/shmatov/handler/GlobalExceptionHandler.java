package ru.shmatov.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.shmatov.exception.*;
import ru.shmatov.response.APIResponse;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<APIResponse> handleValidation(MethodArgumentNotValidException ex) {
        String error = ex.getBindingResult().getFieldError().getDefaultMessage();
        log.warn("Validation error: {}", error, ex);
        return ResponseEntity.badRequest().body(new APIResponse(error));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<APIResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new APIResponse("Неверный тип параметра: " + ex.getMessage()));
    }

    @ExceptionHandler({
            AccountNotFoundException.class,
            BalanceNotFoundException.class,
            ReceiverTransactionNotFoundException.class,
            SenderTransactionNotFoundException.class,
            UserNotFoundException.class
    })
    public ResponseEntity<APIResponse> handleNotFound(RuntimeException ex) {
        log.warn("Not found: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new APIResponse(ex.getMessage()));
    }

    @ExceptionHandler({
            AccountAlreadyExistsException.class,
            DuplicateBalanceNumberException.class,
            UserAlreadyExistsException.class
    })
    public ResponseEntity<APIResponse> handleConflict(RuntimeException ex) {
        log.warn("Conflict: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new APIResponse(ex.getMessage()));
    }

    @ExceptionHandler({
            AccountNotEmptyException.class,
            AlreadyPrimaryBalanceException.class,
            BalanceAccessDeniedException.class,
            BalanceLimitExceededException.class,
            BalanceNotEmptyException.class,
            OnlyOneBalanceException.class,
            PrimaryBalanceDeletionException.class,
            SameBalancesException.class,
            SecurityBalanceNotBelongAccountException.class,
            SecurityBalanceNotBelongTransactionException.class,
            InsufficientFundsException.class,
            CantDeletePrimaryAccountException.class,
    })
    public ResponseEntity<APIResponse> handleForbidden(RuntimeException ex) {
        log.warn("Forbidden: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new APIResponse(ex.getMessage()));
    }

    @ExceptionHandler({
            InvalidConfirmationCodeException.class,
            ConfirmationCodeExpiredException.class,
            BalanceNumberGenerationException.class
    })
    public ResponseEntity<APIResponse> handleBadRequest(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new APIResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse> handleOther(Exception ex) {
        log.error("Unhandled server error", ex); // error level
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new APIResponse("Ошибка сервера: " + ex.getMessage()));
    }
}

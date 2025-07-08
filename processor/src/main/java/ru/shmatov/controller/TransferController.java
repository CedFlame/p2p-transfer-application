package ru.shmatov.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.shmatov.TransactionIdPairDTO;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.request.TransferRequest;
import ru.shmatov.response.APIResponse;
import ru.shmatov.response.TransferResponse;
import ru.shmatov.service.TransferService;

import javax.validation.Valid;
import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Переводы между балансами")
public class TransferController {

    private final TransferService transferService;

    @LogExecutionTime
    @Operation(
            summary = "Создание перевода",
            description = "Инициирует перевод между балансами текущего пользователя. Возвращает сгенерированный код подтверждения и идентификаторы транзакций.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Перевод инициирован",
                            content = @Content(schema = @Schema(implementation = TransferResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Некорректные входные данные"),
                    @ApiResponse(responseCode = "404", description = "Пользователь, аккаунт или баланс не найдены"),
                    @ApiResponse(responseCode = "409", description = "Недостаточно средств")
            }
    )
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @Parameter(hidden = true) Principal principal,
            @RequestBody @Valid TransferRequest req) throws Exception {

        log.info("Initiating transfer from {} to {} by user {} for amount {}",
                req.getFromBalanceNumber(),
                req.getToBalanceNumber(),
                principal.getName(),
                req.getAmount()
        );

        TransferResponse resp = transferService.transfer(
                principal.getName(),
                req.getAmount(),
                req.getFromBalanceNumber(),
                req.getToBalanceNumber()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @LogExecutionTime
    @Operation(
            summary = "Подтверждение перевода",
            description = "Подтверждает перевод, ранее инициированный пользователем, с помощью кода подтверждения.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Перевод подтверждён",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверный код подтверждения"),
                    @ApiResponse(responseCode = "403", description = "Нарушение безопасности"),
                    @ApiResponse(responseCode = "404", description = "Транзакция или баланс не найдены")
            }
    )
    @PostMapping("/confirm")
    public ResponseEntity<APIResponse> confirm(
            @Parameter(hidden = true) Principal principal,
            @Parameter(description = "ID транзакции отправителя") @RequestParam Long senderTxId,
            @Parameter(description = "ID транзакции получателя") @RequestParam Long receiverTxId,
            @Parameter(description = "Код подтверждения") @RequestParam String code) {

        log.info("Confirming transfer by user {} with senderTxId={}, receiverTxId={}, code={}",
                principal.getName(), senderTxId, receiverTxId, code);

        TransactionIdPairDTO pair = new TransactionIdPairDTO(senderTxId, receiverTxId);
        APIResponse resp = transferService.processTransferConfirmation(
                principal.getName(),
                pair,
                code
        );
        return ResponseEntity.ok(resp);
    }
}

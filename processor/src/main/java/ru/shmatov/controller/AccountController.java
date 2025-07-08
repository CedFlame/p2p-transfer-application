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
import ru.shmatov.AccountAndBalancesPairDTO;
import ru.shmatov.AccountMasterBalanceNumberPairDTO;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.request.AccountCreateRequest;
import ru.shmatov.request.BalanceCreateRequest;
import ru.shmatov.response.AccountViewResponse;
import ru.shmatov.service.AccountService;

import javax.validation.Valid;
import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Управление аккаунтом и балансами")
public class AccountController {

    private final AccountService accountService;

    @LogExecutionTime
    @Operation(
            summary = "Создание аккаунта",
            description = "Создаёт новый аккаунт и мастер-баланс для пользователя",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Аккаунт успешно создан",
                            content = @Content(schema = @Schema(implementation = AccountMasterBalanceNumberPairDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Невалидный запрос"),
                    @ApiResponse(responseCode = "409", description = "Аккаунт уже существует")
            }
    )
    @PostMapping
    public ResponseEntity<AccountMasterBalanceNumberPairDTO> create(
            @RequestBody @Valid AccountCreateRequest request) {
        log.info("Creating new account for user: {}", request.getUserUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.create(request));
    }

    @LogExecutionTime
    @Operation(
            summary = "Удаление аккаунта",
            description = "Удаляет аккаунт и все связанные балансы, если на них нет средств",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Аккаунт и балансы удалены",
                            content = @Content(schema = @Schema(implementation = AccountAndBalancesPairDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Пользователь или аккаунт не найден"),
                    @ApiResponse(responseCode = "409", description = "Баланс не пустой")
            }
    )
    @DeleteMapping
    public ResponseEntity<AccountAndBalancesPairDTO> delete(Principal principal) {
        log.info("Deleting account for user: {}", principal.getName());
        return ResponseEntity.ok(accountService.delete(principal.getName()));
    }

    @LogExecutionTime
    @Operation(
            summary = "Получить данные об аккаунте",
            description = "Возвращает информацию об аккаунте, всех балансах и транзакциях пользователя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Информация получена",
                            content = @Content(schema = @Schema(implementation = AccountViewResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден")
            }
    )
    @GetMapping
    public ResponseEntity<AccountViewResponse> show(Principal principal) {
        log.info("Fetching account view for user: {}", principal.getName());
        return ResponseEntity.ok(accountService.getAccountView(principal.getName()));
    }

    @LogExecutionTime
    @Operation(
            summary = "Создание дополнительного баланса",
            description = "Добавляет новый баланс к существующему аккаунту пользователя",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Баланс успешно создан",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Превышен лимит балансов")
            }
    )
    @PostMapping("/balances")
    public ResponseEntity<String> createBalance(
            Principal principal,
            @RequestBody @Valid BalanceCreateRequest body) {

        log.info("Creating new balance for user: {}", principal.getName());
        String number = accountService.createBalance(principal.getName(), body.initialBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(number);
    }

    @LogExecutionTime
    @Operation(
            summary = "Удаление баланса",
            description = "Удаляет баланс по его номеру, если на нем нет средств и он не единственный",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Баланс удалён",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "404", description = "Баланс не найден"),
                    @ApiResponse(responseCode = "409", description = "Баланс единственный или не пустой")
            }
    )
    @DeleteMapping("/balances/{balanceNumber}")
    public ResponseEntity<String> deleteBalance(
            Principal principal,
            @Parameter(description = "Номер удаляемого баланса") @PathVariable String balanceNumber) {

        log.info("Deleting balance {} for user: {}", balanceNumber, principal.getName());
        String number = accountService.deleteBalance(principal.getName(), balanceNumber);
        return ResponseEntity.ok(number);
    }

    @LogExecutionTime
    @Operation(
            summary = "Смена основного баланса",
            description = "Устанавливает указанный баланс как основной для аккаунта пользователя",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Основной баланс обновлён",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "404", description = "Баланс не найден"),
                    @ApiResponse(responseCode = "409", description = "Баланс уже является основным")
            }
    )
    @PatchMapping("/balances/{balanceNumber}/primary")
    public ResponseEntity<String> switchPrimary(
            Principal principal,
            @Parameter(description = "Номер баланса, который будет основным") @PathVariable String balanceNumber) {

        log.info("Switching primary balance to {} for user: {}", balanceNumber, principal.getName());
        String oldNumber = accountService.switchPrimaryBalance(principal.getName(), balanceNumber);
        return ResponseEntity.ok(oldNumber);
    }
}

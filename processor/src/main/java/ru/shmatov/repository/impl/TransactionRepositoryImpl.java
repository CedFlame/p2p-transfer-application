package ru.shmatov.repository.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import ru.shmatov.annotation.LogExecutionTime;
import ru.shmatov.enums.TransactionStatusEnum;
import ru.shmatov.enums.TransactionType;
import ru.shmatov.model.Transaction;
import ru.shmatov.repository.TransactionRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {

    private final JdbcTemplate jdbc;
    private final RowMapper<Transaction> mapper = (rs, n) -> mapTx(rs);

    @Override
    public Optional<Transaction> findById(Long id) {
        try {
            return Optional.ofNullable(
                    jdbc.queryForObject(
                            "SELECT * FROM transactions WHERE id = ?",
                            mapper, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<Transaction> findAllByBalanceId(Long balanceId) {
        return jdbc.query(
                "SELECT * FROM transactions WHERE balance_id = ?",
                mapper, balanceId);
    }

    @LogExecutionTime
    @Override
    public Long save(Transaction t) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO transactions
                      (balance_id, amount, transaction_type, transaction_status,
                       created_at, receiver_balance_id, receiver_transaction_id)
                    VALUES (?,?,?,?,?,?,?)
                    """, new String[]{"id"});
            ps.setLong(1, t.getBalanceId());
            ps.setLong(2, t.getAmount());
            ps.setString(3, t.getTransactionType().name());
            ps.setString(4, t.getTransactionStatus().name());
            ps.setLong(5, t.getCreatedAt());
            ps.setLong(6, t.getReceiverBalanceId());
            if (t.getReceiverTransactionId() != null)
                ps.setLong(7, t.getReceiverTransactionId());
            else
                ps.setNull(7, java.sql.Types.BIGINT);
            return ps;
        }, kh);
        Long id = Objects.requireNonNull(kh.getKey()).longValue();
        log.info("Transaction saved with id={}", id);
        return id;
    }

    @LogExecutionTime
    @Override
    public void updateStatus(Long transactionId, TransactionStatusEnum newStatus) {
        jdbc.update(
                "UPDATE transactions SET transaction_status = ? WHERE id = ?",
                newStatus.name(), transactionId
        );
        log.info("Transaction status updated: id={}, newStatus={}", transactionId, newStatus);
    }

    @LogExecutionTime
    @Override
    public void updateReceiverTransactionId(Long transactionId, Long receiverTransactionId) {
        jdbc.update(
                "UPDATE transactions SET receiver_transaction_id = ? WHERE id = ?",
                receiverTransactionId, transactionId
        );
        log.info("Receiver transaction ID updated: id={}, receiverTxId={}", transactionId, receiverTransactionId);
    }

    @Override
    public boolean existsById(Long id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE id = ?",
                Integer.class,
                id
        );
        return count != null && count > 0;
    }

    private Transaction mapTx(ResultSet rs) throws SQLException {
        return Transaction.builder()
                .id(rs.getLong("id"))
                .balanceId(rs.getLong("balance_id"))
                .amount(rs.getLong("amount"))
                .transactionType(TransactionType.valueOf(rs.getString("transaction_type")))
                .transactionStatus(TransactionStatusEnum.valueOf(rs.getString("transaction_status")))
                .createdAt(rs.getLong("created_at"))
                .receiverBalanceId(rs.getLong("receiver_balance_id"))
                .receiverTransactionId(
                        rs.getObject("receiver_transaction_id") == null
                                ? null
                                : rs.getLong("receiver_transaction_id"))
                .build();
    }
}

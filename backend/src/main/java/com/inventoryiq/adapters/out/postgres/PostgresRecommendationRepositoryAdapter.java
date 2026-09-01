package com.inventoryiq.adapters.out.postgres;

import com.inventoryiq.application.port.out.RecommendationRepository;
import com.inventoryiq.domain.model.Recommendation;
import com.inventoryiq.domain.model.RecommendationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Adaptador de salida respaldado por Postgres (Secciones 8.5/8.6/8.7) vía JdbcTemplate, sin ORM. */
public class PostgresRecommendationRepositoryAdapter implements RecommendationRepository {

	private static final RowMapper<Recommendation> ROW_MAPPER = (rs, rowNum) -> new Recommendation(
			rs.getLong("recommendation_id"),
			rs.getLong("product_id"),
			rs.getLong("store_id"),
			rs.getLong("supplier_id"),
			rs.getInt("suggested_quantity"),
			rs.getDate("order_deadline_date").toLocalDate(),
			rs.getString("justification"),
			RecommendationStatus.valueOf(rs.getString("status")),
			rs.getDate("generation_date").toLocalDate(),
			rs.getString("feedback_comment"),
			rs.getDate("feedback_date") != null ? rs.getDate("feedback_date").toLocalDate() : null);

	private final JdbcTemplate jdbcTemplate;
	private final SimpleJdbcInsert insert;

	public PostgresRecommendationRepositoryAdapter(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.insert = new SimpleJdbcInsert(jdbcTemplate)
				.withTableName("recommendations")
				.usingGeneratedKeyColumns("recommendation_id");
	}

	@Override
	public Recommendation save(Recommendation recommendation) {
		return recommendation.recommendationId() == null ? insert(recommendation) : update(recommendation);
	}

	private Recommendation insert(Recommendation r) {
		Map<String, Object> params = new HashMap<>();
		params.put("product_id", r.productId());
		params.put("store_id", r.storeId());
		params.put("supplier_id", r.supplierId());
		params.put("suggested_quantity", r.suggestedQuantity());
		params.put("order_deadline_date", Date.valueOf(r.orderDeadlineDate()));
		params.put("justification", r.justification());
		params.put("status", r.status().name());
		params.put("generation_date", Date.valueOf(r.generationDate()));
		params.put("feedback_comment", r.feedbackComment());
		params.put("feedback_date", r.feedbackDate() != null ? Date.valueOf(r.feedbackDate()) : null);

		Number generatedId = insert.executeAndReturnKey(params);

		return new Recommendation(generatedId.longValue(), r.productId(), r.storeId(), r.supplierId(),
				r.suggestedQuantity(), r.orderDeadlineDate(), r.justification(), r.status(), r.generationDate(),
				r.feedbackComment(), r.feedbackDate());
	}

	private Recommendation update(Recommendation r) {
		jdbcTemplate.update("""
				UPDATE recommendations
				SET product_id = ?, store_id = ?, supplier_id = ?, suggested_quantity = ?, order_deadline_date = ?,
				    justification = ?, status = ?, generation_date = ?, feedback_comment = ?, feedback_date = ?
				WHERE recommendation_id = ?
				""",
				r.productId(), r.storeId(), r.supplierId(), r.suggestedQuantity(),
				Date.valueOf(r.orderDeadlineDate()), r.justification(), r.status().name(),
				Date.valueOf(r.generationDate()), r.feedbackComment(),
				r.feedbackDate() != null ? Date.valueOf(r.feedbackDate()) : null,
				r.recommendationId());

		return r;
	}

	@Override
	public Optional<Recommendation> findById(Long recommendationId) {
		return jdbcTemplate.query("SELECT * FROM recommendations WHERE recommendation_id = ?", ROW_MAPPER, recommendationId)
				.stream()
				.findFirst();
	}

	@Override
	public List<Recommendation> findByFilters(
			Long storeId, Long supplierId, RecommendationStatus status,
			LocalDate generationDateFrom, LocalDate generationDateTo) {
		StringBuilder sql = new StringBuilder("SELECT * FROM recommendations WHERE store_id = ?");
		List<Object> params = new ArrayList<>();
		params.add(storeId);

		if (supplierId != null) {
			sql.append(" AND supplier_id = ?");
			params.add(supplierId);
		}
		if (status != null) {
			sql.append(" AND status = ?");
			params.add(status.name());
		}
		if (generationDateFrom != null) {
			sql.append(" AND generation_date >= ?");
			params.add(Date.valueOf(generationDateFrom));
		}
		if (generationDateTo != null) {
			sql.append(" AND generation_date <= ?");
			params.add(Date.valueOf(generationDateTo));
		}

		return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
	}
}

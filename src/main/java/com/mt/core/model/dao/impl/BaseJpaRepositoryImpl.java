package com.mt.core.model.dao.impl;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.Query;
import javax.persistence.Table;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Subselect;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mt.core.model.annotation.SearchKeyword;
import com.mt.core.model.dao.BaseJpaRepository;
import com.mt.core.model.exception.DAOException;
import com.mt.core.model.exception.InvalidColumnNameMappingException;
import com.mt.core.model.exception.InvalidLogicalOperatorException;
import com.mt.core.model.vo.PageRequestVO;
import com.mt.core.model.vo.PageResponseVO;
import com.mt.core.model.vo.PageRequestVO.Filter;
import com.mt.core.model.vo.PageRequestVO.FilterDescriptor;
import com.mt.core.model.vo.PageRequestVO.SortDescriptor;

/**
 * Base JPA repository implementation
 * 
 * @author ur04192
 *
 * @param <E> Entity class
 * @param <K> Entity id class
 */
public class BaseJpaRepositoryImpl<E, K> extends SimpleJpaRepository<E, K> implements BaseJpaRepository<E, K> {

	protected EntityManager entityManager;

	@Qualifier("mainJdbcTemplate")
	@Autowired
	protected JdbcTemplate jdbcTemplate; // Spring JDBC Template

	/**
	 * DAO Entity Class
	 */
	private final Class<E> persistentClass;

	/**
	 * DAO Entity Table Name: Catalog.TableName
	 */
	private final String persistentTable;

	/**
	 * Entity Class 屬性(field)名稱 與 DB Table 欄位(column)名稱對映
	 */
	private final Map<String, String> fieldColumnMapping;

	/**
	 * 搜尋關鍵字的對象欄位
	 */
	private String[] keywordColumns = {};

	/**
	 * Constructor
	 * 
	 * @param entityInformation
	 * @param entityManager
	 */
	public BaseJpaRepositoryImpl(JpaEntityInformation<E, ?> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
		this.entityManager = entityManager;

		// Entity class
		this.persistentClass = entityInformation.getJavaType();

		// Entity table name
		Table table = persistentClass.getAnnotation(Table.class);
		Subselect subselect = persistentClass.getAnnotation(Subselect.class);

		this.persistentTable = table != null
				? (StringUtils.isBlank(table.catalog()) ? table.name()
						: (table.catalog().concat(".").concat(table.name())))
				: subselect != null ? ("(" + subselect.value() + ")" + ")") : null;

		// Entity Class 屬性與 DB Table 欄位對照表
		this.fieldColumnMapping = this.createEntityColumnMapping();
	}

	@Override
	public String getPersistentTable() {
		return this.persistentTable;
	}

	/**
	 * 建立 Entity Class 屬性與 DB Table 欄位對照表
	 * 
	 * @return
	 */
	private Map<String, String> createEntityColumnMapping() {
		Field[] fields = this.persistentClass.getDeclaredFields();

		// 如父類別有註解 @MappedSuperclass, 將父類別的 properties 也加入
		if (this.persistentClass.getSuperclass().isAnnotationPresent(MappedSuperclass.class)) {
			fields = ArrayUtils.addAll(fields, this.persistentClass.getSuperclass().getDeclaredFields());
		}

		Map<String, String> columnsMap = new HashMap<>();

		for (Field field : fields) {
			if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Id.class)
					|| field.isAnnotationPresent(JoinColumn.class)) {
				String fieldName = field.getName();
				String columnName = fieldName;

				if (field.isAnnotationPresent(Column.class)) {
					Column column = field.getAnnotation(Column.class);
					columnName = StringUtils.defaultIfBlank(column.name(), fieldName);
				} else if (field.isAnnotationPresent(JoinColumn.class)) {
					JoinColumn column = field.getAnnotation(JoinColumn.class);
					columnName = StringUtils.defaultIfBlank(column.name(), fieldName);
				}

				/* 搜集標註 @SearchKeyword 的欄位 */
				if (field.isAnnotationPresent(SearchKeyword.class)) {
					this.keywordColumns = ArrayUtils.add(this.keywordColumns, columnName);
				}

				columnsMap.put(fieldName, columnName);
			}
		}

		return columnsMap;
	}

	/**
	 * 查詢 entity 屬性對映後端資料庫的欄位名稱
	 * 
	 * @param fieldName entity 屬性
	 * @return
	 */
	public String getMappedColumnName(String fieldName) {
		return this.fieldColumnMapping.get(fieldName);
	}

	/**
	 * 查詢資料
	 * 
	 * @param req
	 * @return
	 * @throws DAOException
	 */
	@Override
	public PageResponseVO<E> getRows(PageRequestVO req) throws DAOException {
		return this.getRows(req, this.persistentTable);
	}

	/**
	 * 查詢資料
	 * 
	 * @param req
	 * @param fromTableOrSubselect
	 * @return
	 * @throws DAOException
	 */
	@Override
	public PageResponseVO<E> getRows(PageRequestVO req, String fromTableOrSubselect) throws DAOException {

		try {
			StringBuilder countSQL = new StringBuilder("SELECT count(*) cnt FROM ").append(fromTableOrSubselect);

			Map<String, Object> params = new HashMap<>();
			String whereSQL = "";

			if (req.getFilter() != null) {
				whereSQL = this.getWhereSQL(req.getFilter(), params);
			}

			Query countQuery = this.entityManager.createNativeQuery(countSQL.append(whereSQL).toString());

			for (String key : params.keySet()) {
				countQuery.setParameter(key, params.get(key));
			}

			// 查詢結果數
			Object countResult = countQuery.getSingleResult();
			int count = countResult instanceof BigInteger ? ((BigInteger) countResult).intValue()
					: ((Integer) countResult).intValue();
			List<E> result = Collections.emptyList();

			// 查詢結果列
			if (count > 0) {
				StringBuilder resultSQL = new StringBuilder("SELECT * FROM ").append(fromTableOrSubselect);
				resultSQL.append(whereSQL);

				// 處理排序
				List<SortDescriptor> sortProps = req.getSortProps();

				if (sortProps != null && sortProps.size() > 0) {
					List<String> orderBy = new ArrayList<>();

					for (SortDescriptor sort : sortProps) {
						String columnName = this.getMappedColumnName(sort.getField());

						if (columnName == null) {
							throw new InvalidColumnNameMappingException(
									"找不到對映 " + sort.getField() + " 屬性的 table column");
						}

						orderBy.add(columnName + " " + Direction.fromString(sort.getDir()));
					}

					resultSQL.append(" ORDER BY ").append(StringUtils.join(orderBy, ", "));

					// String orderBySQL = sortProps.stream().map(sort -> {
					// String columnName = this.getMappedColumnName(sort.getField());
					// return columnName + " " + Direction.fromString(sort.getDir());
					// }).collect(Collectors.joining(", ", " ORDER BY ", ""));
					//
					// resultSQL.append(orderBySQL);
				}

				NativeQuery<E> resultQuery = (NativeQuery<E>) this.entityManager.createNativeQuery(resultSQL.toString(),
						this.persistentClass);

				for (String key : params.keySet()) {
					resultQuery.setParameter(key, params.get(key));
				}

				// size > 0 時, 做分頁處理
				if (req.getSize() > 0) {
					result = resultQuery.setMaxResults(req.getSize()).setFirstResult(req.getSize() * req.getPage())
							.getResultList();
				} else { // size 為 0 時, 不做分頁處理 (取全部資料)
					result = resultQuery.getResultList();
				}
			}

			return new PageResponseVO<E>((int) count, result);
		} catch (DAOException e) {
			throw e;
		} catch (Exception e) {
			throw new DAOException("查詢資料列失敗", e);
		}
	}

	/**
	 * 組合 SQL Where 敍述
	 * 
	 * @param filter
	 * @param params
	 * @return
	 * @throws InvalidLogicalOperatorException
	 * @throws DAOException
	 * @throws InvalidColumnNameMappingException
	 */
	private String getWhereSQL(FilterDescriptor filter, Map<String, Object> params)
			throws InvalidLogicalOperatorException, DAOException, InvalidColumnNameMappingException {

		String paramPrefix = "";
		StringBuilder whereSQL = new StringBuilder(" WHERE 1=1 AND (")
				.append(this.getCondSQL(filter, params, paramPrefix)).append(")");

		return whereSQL.toString();
	}

	/**
	 * 組合 SQL 查詢條件
	 * 
	 * @param filter
	 * @param params
	 * @param paramPrefix
	 * @return
	 * @throws InvalidLogicalOperatorException
	 * @throws DAOException
	 * @throws InvalidColumnNameMappingException
	 */
	private String getCondSQL(FilterDescriptor filter, Map<String, Object> params, String paramPrefix)
			throws InvalidLogicalOperatorException, DAOException, InvalidColumnNameMappingException {
		StringBuilder sql = new StringBuilder("");
		Filter subFilter = filter.getFilter();

		if (subFilter != null) {
			List<FilterDescriptor> subFilters = subFilter.getSubFilters();

			if (subFilters.size() == 1) {
				return this.getCondSQL(subFilters.get(0), params, paramPrefix + "_0");
			} else {
				String logic = subFilter.getLogic();
				String logicSQL = null;

				if ("and".equals(logic)) {
					sql.append("1=1");
					logicSQL = " AND ";
				} else {
					sql.append("1=0");
					logicSQL = " OR ";
				}

				for (int i = 0; i < subFilters.size(); i++) {
					FilterDescriptor f = subFilters.get(i);

					sql.append(logicSQL).append("(").append(this.getCondSQL(f, params, paramPrefix + "_" + i))
							.append(")");
				}
			}
		} else {
			String field = filter.getField();
			Object value = filter.getValue();
			String operator = filter.getOperator();
			int paramIdx = 0;

			if (value != null) {
				String param = paramPrefix + "__" + (paramIdx++);

				// 搜尋關鍵字
				if ("keyword".equals(field)) {
					params.put(param, "%" + value.toString() + "%");

					if (this.keywordColumns == null || this.keywordColumns.length == 0) {
						throw new DAOException("未設定查詢關鍵字所包含的欄位");
					} else {
						sql.append("1=0");

						for (String column : this.keywordColumns) {
							sql.append(" OR ").append(column).append(" LIKE :").append(param);
						}
					}
				} else { // 其他過濾條件
					String columnName = this.getMappedColumnName(filter.getField());

					if (columnName == null) {
						throw new InvalidColumnNameMappingException("找不到對映 " + filter.getField() + " 屬性的 table column");
					}

					switch (operator) {

						case "contains":
							params.put(param, "%" + value.toString() + "%");
							sql.append(columnName).append(" LIKE :").append(param);
							break;
						case "between":
							List<?> values = (List<?>) value;
							Object from = values.get(0);
							Object to = values.get(1);

							if (from != null && to != null) {
								String param2 = paramPrefix + "__" + (paramIdx++);
								params.put(param, from);
								params.put(param2, to);

								sql.append(columnName).append(" BETWEEN :").append(param).append(" AND :")
										.append(param2);
							} else if (from != null) {
								params.put(param, from);
								sql.append(columnName).append(" >= :").append(param);
							} else if (to != null) {
								params.put(param, to);
								sql.append(columnName).append(" <= :").append(param);
							}
							break;
						case "gt":
							params.put(param, value);
							sql.append(columnName).append(" > :").append(param);
							break;
						case "ge":
							params.put(param, value);
							sql.append(columnName).append(" >= :").append(param);
							break;
						case "lt":
							params.put(param, value);
							sql.append(columnName).append(" < :").append(param);
							break;
						case "le":
							params.put(param, value);
							sql.append(columnName).append(" <= :").append(param);
							break;
						case "eq":
							String sqlOperator = value instanceof List || value.getClass().isArray() ? "IN" : "=";
							params.put(param, value);
							sql.append(columnName).append(" ").append(sqlOperator).append(" :").append(param);
							break;
						default:
							throw new InvalidLogicalOperatorException("無效的邏輯運算子 " + operator);
					}

				}
			}
		}

		return sql.toString();
	}
}

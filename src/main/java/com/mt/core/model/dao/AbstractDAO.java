package com.mt.core.model.dao;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.jdbc.core.JdbcTemplate;

import com.mt.core.model.annotation.SearchKeyword;
import com.mt.core.model.exception.DAOException;
import com.mt.core.model.exception.InvalidColumnNameMappingException;
import com.mt.core.model.exception.InvalidLogicalOperatorException;
import com.mt.core.model.vo.PageRequestVO;
import com.mt.core.model.vo.PageResponseVO;
import com.mt.core.model.vo.PageRequestVO.Filter;
import com.mt.core.model.vo.PageRequestVO.FilterDescriptor;
import com.mt.core.model.vo.PageRequestVO.SortDescriptor;

public abstract class AbstractDAO<E, K> {
	@Qualifier("mainHibernateSessionFactory")
	@Autowired
	private SessionFactory sessionFactory;

	@Qualifier("mainJdbcTemplate")
	@Autowired
	protected JdbcTemplate jdbcTemplate;

	// DAO Entity Class
	private final Class<E> persistentClass = (Class) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];

	// DAO Entity Table Name: Catalog.TableName
	private final String persistentTable;

	// Entity Class 屬性與 DB Table 欄位對照表
	private final Map<String, String> columnsMap;

	// 搜尋關鍵字的對象欄位
	private String[] keywordColumns = new String[0];

	public AbstractDAO() {
		// Entity table name
		Table table = (Table) this.persistentClass.getAnnotation(Table.class);
		this.persistentTable = StringUtils.isBlank(table.catalog()) ? table.name() : table.catalog().concat(".").concat(table.name());

		// Entity Class 屬性與 DB Table 欄位對照表
		this.columnsMap = this.createEntityColumnMapping();
	}

	protected Session getSession() {
		return this.sessionFactory.getCurrentSession();
	}

	public Class<E> getPersistentClass() {
		return this.persistentClass;
	}

	public String getPersistentTable() {
		return this.persistentTable;
	}

	public int deleteByKey(K key) throws DAOException {
		try {
			StringBuilder sql = (new StringBuilder("DELETE FROM ")).append(this.getPersistentTable()).append(" WHERE id=:id");
			int count = this.getSession().createNativeQuery(sql.toString()).setParameter("id", key).executeUpdate();
			return count;
		} catch (Exception var4) {
			throw new DAOException("主鍵刪除資料失敗", var4);
		}
	}

	public int deleteByKeys(K[] keys) throws DAOException {
		try {
			StringBuilder sql = (new StringBuilder("DELETE FROM ")).append(this.getPersistentTable()).append(" WHERE id IN :ids");
			int count = this.getSession().createNativeQuery(sql.toString()).setParameterList("ids", keys).executeUpdate();
			return count;
		} catch (Exception var4) {
			throw new DAOException("多個主鍵刪除資料失敗", var4);
		}
	}

	private Map<String, String> createEntityColumnMapping() {
		Map<String, String> columnsMap = new HashMap();
		Field[] var2 = this.persistentClass.getDeclaredFields();
		int var3 = var2.length;

		for (int var4 = 0; var4 < var3; ++var4) {
			Field field = var2[var4];
			if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(JoinColumn.class)) {
				String fieldName = field.getName();
				String columnName = fieldName;
				if (field.isAnnotationPresent(Column.class)) {
					Column column = (Column) field.getAnnotation(Column.class);
					columnName = (String) StringUtils.defaultIfBlank(column.name(), fieldName);
				} else if (field.isAnnotationPresent(JoinColumn.class)) {
					JoinColumn column = (JoinColumn) field.getAnnotation(JoinColumn.class);
					columnName = (String) StringUtils.defaultIfBlank(column.name(), fieldName);
				}

				if (field.isAnnotationPresent(SearchKeyword.class)) {
					this.keywordColumns = (String[]) ArrayUtils.add(this.keywordColumns, columnName);
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
		return (String) this.columnsMap.get(fieldName);
	}

	public E saveOrUpdate(E entity) throws DAOException {
		Session session = this.getSession();

		try {
			this.getSession().saveOrUpdate(entity);
			return entity;
		} catch (Exception var4) {
			session.clear();
			throw new DAOException("entity 儲存失敗", var4);
		}
	}

	public E getByKey(K key) throws DAOException {
		try {
			return this.getSession().find(this.persistentClass, key);
		} catch (Exception var3) {
			throw new DAOException("主鍵查詢失敗", var3);
		}
	}

	/**
	 * 查詢資料
	 * 
	 * @param req
	 * @return
	 * @throws DAOException
	 */
	public PageResponseVO<E> getRows(PageRequestVO req) throws DAOException {
		return this.getRows(req, this.persistentTable, (Map) null);
	}

	/**
	 * 查詢資料
	 * 
	 * @param req
	 * @param fromTableOrSubselect
	 * @return
	 * @throws DAOException
	 */
	public PageResponseVO<E> getRows(PageRequestVO req, String fromTableOrSubselect) throws DAOException {
		return this.getRows(req, fromTableOrSubselect, (Map) null);
	}

	public PageResponseVO<E> getRows(PageRequestVO req, String fromTableOrSubselect, Map<String, Object> subSelectParams) throws DAOException {
		try {
			Session session = this.getSession();
			StringBuilder countSQL = (new StringBuilder("SELECT count(*) cnt FROM ")).append(fromTableOrSubselect);
			Map<String, Object> params = new HashMap<>();
			String whereSQL = "";

			if (req.getFilter() != null) {
				whereSQL = this.getWhereSQL(req.getFilter(), params);
			}

			NativeQuery<Integer> countQuery = session.createNativeQuery(countSQL.append(whereSQL).toString()).addScalar("cnt", StandardBasicTypes.INTEGER);

			for (String key : params.keySet()) {
				countQuery.setParameter(key, params.get(key));
			}

			if (subSelectParams != null) {
				for (String key : subSelectParams.keySet()) {
					countQuery.setParameter(key, subSelectParams.get(key));
				}
			}

			// 查詢結果數
			Integer count = (Integer) countQuery.uniqueResult();
			List<E> result = Collections.emptyList();

			// 查詢結果列
			if (count > 0) {
				StringBuilder resultSQL = (new StringBuilder("SELECT * FROM ")).append(fromTableOrSubselect);
				resultSQL.append(whereSQL);

				// 處理排序
				List<SortDescriptor> sortProps = req.getSortProps();

				if (sortProps != null) {
					String orderBySQL = sortProps.stream().map(sort -> {
						String columnName = this.getMappedColumnName(sort.getField());
						return columnName + " " + Direction.fromString(sort.getDir());
					}).collect(Collectors.joining(", ", " ORDER BY ", ""));

					resultSQL.append(orderBySQL);
				}

				NativeQuery resultQuery = session.createNativeQuery(resultSQL.toString(), this.persistentClass);

				for (String key : params.keySet()) {
					resultQuery.setParameter(key, params.get(key));
				}

				if (subSelectParams != null) {
					for (String key : subSelectParams.keySet()) {
						resultQuery.setParameter(key, subSelectParams.get(key));
					}
				}

				// size > 0 時, 做分頁處理
				if (req.getSize() > 0) {
					result = resultQuery.setMaxResults(req.getSize()).setFirstResult(req.getSize() * req.getPage()).list();
				} else { // size 為 0 時, 不做分頁處理 (取全部資料)
					result = resultQuery.list();
				}
			}

			return new PageResponseVO(count, result);
		} catch (Exception var16) {
			throw new DAOException("查詢資料列失敗", var16);
		}
	}

	public String buildSqlConditions(FilterDescriptor filter, Map<String, Object> params) throws InvalidLogicalOperatorException, InvalidColumnNameMappingException {
		return this.getCondSQL(filter, params, "");
	}

	/**
	 * 組合 SQL Where 敍述
	 * 
	 * @param filter
	 * @param params
	 * @return
	 * @throws InvalidLogicalOperatorException
	 */
	private String getWhereSQL(FilterDescriptor filter, Map<String, Object> params) throws InvalidLogicalOperatorException, InvalidColumnNameMappingException {
		String paramPrefix = "";
		StringBuilder whereSQL = (new StringBuilder(" WHERE 1=1 AND (")).append(this.getCondSQL(filter, params, paramPrefix)).append(")");
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
	 * @throws InvalidColumnNameMappingException
	 */
	private String getCondSQL(FilterDescriptor filter, Map<String, Object> params, String paramPrefix) throws InvalidLogicalOperatorException, InvalidColumnNameMappingException {
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

					sql.append(logicSQL).append("(").append(this.getCondSQL(f, params, paramPrefix + "_" + i)).append(")");
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

					} else {
						sql.append("1=0");

						for (String column : this.keywordColumns) {
							sql.append(" OR ").append(column).append(" LIKE :").append(param);
						}
					}
				} else { // 其他過濾條件
					String columnName = this.getMappedColumnName(field);
					if (columnName == null) {
						throw new InvalidColumnNameMappingException("找不到對映 " + field + " 的資料庫欄位名稱");
					}

					switch (operator) {

					case "contains":
						if (value instanceof List) {
							params.put(param, value);
							sql.append(columnName).append(" IN :").append(param);
						} else {
							params.put(param, "%" + value.toString() + "%");
							sql.append(columnName).append(" LIKE :").append(param);
						}
						break;
					case "between":
						List<?> values = (List<?>) value;
						Object from = values.get(0);
						Object to = values.get(1);

						if (from != null && to != null) {
							String param2 = paramPrefix + "__" + (paramIdx++);
							params.put(param, from);
							params.put(param2, to);

							sql.append(columnName).append(" BETWEEN :").append(param).append(" AND :").append(param2);
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
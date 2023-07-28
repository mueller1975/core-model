package com.mt.core.model.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

import com.mt.core.model.dao.BaseJpaRepository;
import com.mt.core.model.exception.DAOException;
import com.mt.core.model.exception.ServiceException;
import com.mt.core.model.vo.ExportColumnVO;
import com.mt.core.model.vo.ExportRequestVO;
import com.mt.core.model.vo.PageRequestVO;
import com.mt.core.model.vo.PageResponseVO;

/**
 * Abstract Data Access Service Class
 * 
 * @author Mueller Tsai
 *
 * @param <E>   Entity Class
 * @param <DTO> DTO Class
 * @param <DAO> DAO class
 */
public class AbstractService<E, K, DTO, DAO extends BaseJpaRepository<E, K>> {

	protected final Class<E> persistentClass;
	protected final Class<DTO> dtoClass;

	protected final Map<String, Field> fieldsMapping;

	@Autowired
	protected DAO dao;

	@Autowired
	protected ModelMapper modelMapper;

	public AbstractService() {
		// Entity class
		this.persistentClass = (Class<E>) ((ParameterizedType) getClass().getGenericSuperclass())
				.getActualTypeArguments()[0];
		this.dtoClass = (Class<DTO>) ((ParameterizedType) getClass().getGenericSuperclass())
				.getActualTypeArguments()[2];

		this.fieldsMapping = this.createFieldsMapping();
	}

	/**
	 * 建立 Entity Class 屬性與 DB Table 欄位對照表
	 * 
	 * @return
	 */
	private Map<String, Field> createFieldsMapping() {
		Field[] fields = this.persistentClass.getDeclaredFields();

		// 如父類別有註解 @MappedSuperclass, 將父類別的 properties 也加入
		if (this.persistentClass.getSuperclass().isAnnotationPresent(MappedSuperclass.class)) {
			fields = ArrayUtils.addAll(fields, this.persistentClass.getSuperclass().getDeclaredFields());
		}

		Map<String, Field> map = new HashMap<>();

		for (Field field : fields) {
			if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Id.class)
					|| field.isAnnotationPresent(JoinColumn.class)) {

				map.put(field.getName(), field);
			}
		}

		return map;
	}

	/**
	 * 儲存單筆資料列
	 * 
	 * @param dto
	 * @return
	 * @throws ServiceException
	 */
	public E save(DTO dto) throws ServiceException {
		try {
			E entity = this.mapDTOToEntity(dto);
			return this.dao.save(entity);
		} catch (Exception e) {
			throw new ServiceException("實體 " + this.persistentClass.getSimpleName() + " 資料 INSERT 失敗", e);
		}
	}

	/**
	 * 儲存多筆資料列 (DTO)
	 * 
	 * @param dtoList
	 * @throws ServiceException
	 */
	public List<E> save(List<DTO> dtoList) throws ServiceException {
		try {
			List<E> entities = new ArrayList<>();

			for (DTO dto : dtoList) {
				entities.add(mapDTOToEntity(dto));
			}

			return this.saveAll(entities);
		} catch (Exception e) {
			throw new ServiceException("實體 " + this.persistentClass.getSimpleName() + " 資料 (多筆) INSERT 失敗", e);
		}
	}

	/**
	 * 儲存多筆資料列 (DTO)
	 * 
	 * @param entityList
	 * @return
	 * @throws ServiceException
	 */
	public List<E> saveAll(List<E> entityList) {
		return this.dao.saveAll(entityList);
	}

	/**
	 * 以 id 查詢資料列
	 * 
	 * @param id
	 * @return
	 * @throws ServiceException
	 */
	public DTO find(K id) throws ServiceException {
		try {
			E entity = this.dao.findById(id).orElse(null); // .orElseThrow(() -> new NoRecordException("查詢不到指定主鍵的資料列"));
			return entity != null ? this.mapEntityToDTO(entity) : null;
		} catch (Exception e) {
			throw new ServiceException("以主鍵查詢資料發生錯誤", e);
		}
	}

	/**
	 * 查詢資料
	 * 
	 * @param requestVO
	 * @return
	 * @throws DAOException
	 * @throws ServiceException
	 */
	public PageResponseVO<E> getData(PageRequestVO requestVO) throws ServiceException {
		try {
			return dao.getRows(requestVO);
		} catch (Throwable e) {
			throw new ServiceException("查詢列表資料發生錯誤", e);
		}
	}

	/**
	 * 查詢資料 (回傳 DTO)
	 * 
	 * @param requestVO
	 * @return
	 * @throws DAOException
	 * @throws ServiceException
	 */
	public PageResponseVO<DTO> getDataAsDTO(PageRequestVO requestVO) throws ServiceException {
		try {
			PageResponseVO<E> response = dao.getRows(requestVO);
			return mapEntityToDTO(response);
		} catch (Throwable e) {
			throw new ServiceException("查詢列表資料發生錯誤", e);
		}
	}

	/**
	 * Collection of Entity mapped to collection of DTO
	 * 
	 * @param vo
	 * @return
	 */
	public PageResponseVO<DTO> mapEntityToDTO(PageResponseVO<E> vo) {
		List<DTO> rows = vo.getRows() == null ? null
				: vo.getRows().stream().map(entity -> modelMapper.map(entity, dtoClass)).collect(Collectors.toList());

		PageResponseVO<DTO> mappedVO = new PageResponseVO<DTO>();
		mappedVO.setTotal(vo.getTotal());
		mappedVO.setRows(rows);

		return mappedVO;
	}

	/**
	 * Entity object mapped to DTO object
	 * 
	 * @param entity
	 * @return
	 * @throws ServiceException
	 */
	public DTO mapEntityToDTO(E entity) throws ServiceException {
		try {
			return this.modelMapper.map(entity, this.dtoClass);
		} catch (Exception e) {
			throw new ServiceException("Entity 物件轉換為 DTO 物件失敗", e);
		}
	}

	/**
	 * DTO object mapped to Entity object
	 * 
	 * @param dto
	 * @return
	 * @throws ServiceException
	 */
	public E mapDTOToEntity(DTO dto) throws ServiceException {
		try {
			E entity = this.modelMapper.map(dto, this.persistentClass);
			return entity;
		} catch (Exception e) {
			throw new ServiceException("DTO 物件轉換為 Entity 物件失敗", e);
		}
	}

	/**
	 * Entity objects mapped to DTO objects
	 * 
	 * @param entities
	 * @return
	 * @throws ServiceException
	 */
	public List<DTO> mapEntitiesToDTOs(List<E> entities) throws ServiceException {
		try {
			return entities == null ? null
					: entities.stream().map(entity -> modelMapper.map(entity, dtoClass)).collect(Collectors.toList());
		} catch (Exception e) {
			throw new ServiceException("Entity 物件轉換為 DTO 物件失敗", e);
		}
	}

	/**
	 * DTO objects mapped to Entity objects
	 * 
	 * @param dtos
	 * @return
	 * @throws ServiceException
	 */
	public List<E> mapDTOsToEntities(List<DTO> dtos) throws ServiceException {
		try {
			return dtos == null ? null
					: dtos.stream().map(dto -> modelMapper.map(dto, persistentClass)).collect(Collectors.toList());
		} catch (Exception e) {
			throw new ServiceException("DTO 物件轉換為 Entity 物件失敗", e);
		}
	}

	/**
	 * 匯出 Excel
	 * 
	 * @param requestVO
	 * @param sheetTitle
	 * @return
	 * @throws ServiceException
	 */
	public ByteArrayOutputStream excelExport(ExportRequestVO requestVO, String sheetTitle) throws ServiceException {
		try {
			PageResponseVO<E> data = this.getData(requestVO);
			return this.export(data.getRows(), requestVO.getExportColumns(), sheetTitle);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
				| IOException e) {
			throw new ServiceException("Excel 內容創建發生錯誤", e);
		}
	}

	/**
	 * 匯出 Excel
	 * 
	 * @param rows
	 * @param columns
	 * @param sheetTitle
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	public ByteArrayOutputStream export(List<E> rows, List<ExportColumnVO> columns, String sheetTitle)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException,
			IOException {
		XSSFWorkbook wb = new XSSFWorkbook();
		Sheet sheet = wb.createSheet(sheetTitle);
		sheet.setDefaultColumnWidth(15);
		IndexedColorMap colorMap = wb.getStylesSource().getIndexedColors();
		XSSFColor color = new XSSFColor(colorMap);
		color.setARGBHex("f1d476");
		XSSFCellStyle headerStyle = wb.createCellStyle();
		headerStyle.setFillForegroundColor(color);
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		int colPos = 0;
		List<Field> fields = new ArrayList<>();
		Row header = sheet.createRow(0);
		Iterator<ExportColumnVO> iterator = columns.iterator();

		while (iterator.hasNext()) {
			ExportColumnVO column = (ExportColumnVO) iterator.next();
			Cell cell = header.createCell(colPos++);
			cell.setCellValue(column.getName());
			cell.setCellStyle(headerStyle);
			Field field = this.fieldsMapping.get(column.getProp());
			field.setAccessible(true);
			fields.add(field);
		}

		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdfDatetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		int rowPos = 1;

		for (E row : rows) {
			Row xlsRow = sheet.createRow(rowPos++);
			colPos = 0;

			for (Field field : fields) {
				Object value = field.get(row);
				Date d;
				Cell cell;
				if (value instanceof java.sql.Date) {
					d = new Date(((java.sql.Date) value).getTime());
					cell = xlsRow.createCell(colPos++);
					cell.setCellValue(sdfDate.format(d));
				} else if (value instanceof Timestamp) {
					d = new Date(((Timestamp) value).getTime());
					cell = xlsRow.createCell(colPos++);
					cell.setCellValue(sdfDatetime.format(d));
				} else if (value instanceof Integer) {
					xlsRow.createCell(colPos++).setCellValue((double) (Integer) value);
				} else if (value instanceof Float) {
					xlsRow.createCell(colPos++).setCellValue((Double) value);
				} else if (value instanceof List) {
					xlsRow.createCell(colPos++).setCellValue((String) ((List) value).stream().map((e) -> {
						return e.toString();
					}).collect(Collectors.joining(", ")));
				} else {
					xlsRow.createCell(colPos++).setCellValue(value == null ? "" : String.valueOf(value));
				}
			}
		}

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		wb.write(bos);
		bos.flush();
		wb.close();
		return bos;
	}
}

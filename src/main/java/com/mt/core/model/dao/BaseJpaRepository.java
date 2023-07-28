package com.mt.core.model.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.mt.core.model.exception.DAOException;
import com.mt.core.model.vo.PageRequestVO;
import com.mt.core.model.vo.PageResponseVO;

/**
 * Base JPA class
 * 
 * @author ur04192
 *
 * @param <E> Entity class
 * @param <K> Entity id class
 */
@NoRepositoryBean
public interface BaseJpaRepository<E, K> extends JpaRepository<E, K> {

	public PageResponseVO<E> getRows(PageRequestVO req) throws DAOException;

	public PageResponseVO<E> getRows(PageRequestVO req, String fromTableOrSubselect) throws DAOException;
	
	public String getPersistentTable();
}

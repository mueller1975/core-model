package com.mt.core.model.generator;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

public class UUIDGenerator implements IdentifierGenerator {

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		String id = (String) session.getEntityPersister(object.getClass().getName(), object).getIdentifier(object,
				session);
		return StringUtils.isNotBlank(id) ? id : UUID.randomUUID().toString();
	}

}

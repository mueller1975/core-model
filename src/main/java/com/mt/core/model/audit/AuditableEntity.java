package com.mt.core.model.audit;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

	@CreatedBy
	@Column(updatable = false)
	private String creator;

	@LastModifiedBy
	@Column
	private String modifier;

	@CreatedDate
	@Temporal(value = TemporalType.TIMESTAMP)
	@Column(name = "CREATE_TIME", updatable = false)
	private Date creationTime;

	@LastModifiedDate
	@Temporal(value = TemporalType.TIMESTAMP)
	@Column(name = "MODIFY_TIME")
	private Date modificationTime;
}

/*
 * @(#)IDomainAudit.java Copyright 2011 LSQ Systems, Inc. All rights reserved.
 */
package com.lsq.systems.metagen58;

import java.util.Date;

/**
 * This interface represents an audit entity.
 * 
 * @author Viet Trinh
 * @since 0.7
 */
public interface AuditInterface {

	public Date getAuditDate();

	public void setAuditDate(Date audit_date);

}

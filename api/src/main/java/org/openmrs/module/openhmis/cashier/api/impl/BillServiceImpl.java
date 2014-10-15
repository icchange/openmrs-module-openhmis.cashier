/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.openhmis.cashier.api.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Patient;
import org.openmrs.annotation.Authorized;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.cashier.api.IBillService;
import org.openmrs.module.openhmis.cashier.api.IReceiptNumberGenerator;
import org.openmrs.module.openhmis.cashier.api.ReceiptNumberGeneratorFactory;
import org.openmrs.module.openhmis.cashier.api.model.Bill;
import org.openmrs.module.openhmis.cashier.api.search.BillSearch;
import org.openmrs.module.openhmis.cashier.api.util.CashierPrivilegeConstants;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.commons.api.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.openhmis.commons.api.entity.security.IEntityAuthorizationPrivileges;
import org.openmrs.module.openhmis.commons.api.f.Action1;
import org.openmrs.module.openhmis.inventory.api.impl.LocationUserDataServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.AccessControlException;
import java.util.List;

import org.openmrs.util.RoleConstants;
import org.openmrs.Location;
import org.openmrs.User;

@Transactional
public class BillServiceImpl extends BaseEntityDataServiceImpl<Bill> implements IEntityAuthorizationPrivileges, IBillService {
	
	private static final int MAX_LENGTH_RECEIPT_NUMBER = 255;
    private static final Log LOG = LogFactory.getLog(BillServiceImpl.class);
    private static final String LOCATIONPROPERTY = "defaultLocation";

	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return this;
	}

	@Override
	protected void validate(Bill bill) throws APIException {
	}

	/**
	 * Saves the bill to the database, creating a new bill or updating an existing one.
	 *
	 * @param bill The bill to be saved.
	 * @return The saved bill.
	 * @should Generate a new receipt number if one has not been defined.
	 * @should Not generate a receipt number if one has already been defined.
	 * @should Throw APIException if receipt number cannot be generated.
	 */
	@Override
	@Authorized( { CashierPrivilegeConstants.MANAGE_BILLS } )
	@Transactional
	public Bill save(Bill bill) throws APIException {
		if (bill == null) {
			throw new NullPointerException("The bill must be defined.");
		}
		
		/* Check for refund.
		 * A refund is given when the total of the bill's line items is negative.
		 */
		if (bill.getTotal().compareTo(BigDecimal.ZERO) < 0 && !Context.hasPrivilege(CashierPrivilegeConstants.REFUND_MONEY)) {
			throw new AccessControlException("Access denied to give a refund.");
		}
		IReceiptNumberGenerator generator = ReceiptNumberGeneratorFactory.getGenerator();
		if (generator == null) {
			LOG.warn("No receipt number generator has been defined.  Bills will not be given a receipt number until one is defined.");
		} else {
			if (StringUtils.isEmpty(bill.getReceiptNumber())) {
				bill.setReceiptNumber(generator.generateNumber(bill));
			}
		}

		return super.save(bill);
	}

	@Override
	@Authorized( { CashierPrivilegeConstants.VIEW_BILLS } )
	@Transactional(readOnly = true)
	public Bill getBillByReceiptNumber(String receiptNumber) throws APIException {
		if (StringUtils.isEmpty(receiptNumber)) {
			throw new IllegalArgumentException("The receipt number must be defined.");
		}
		if (receiptNumber.length() > MAX_LENGTH_RECEIPT_NUMBER) {
			throw new IllegalArgumentException("The receipt number must be less than 256 characters.");
		}

		Criteria criteria = repository.createCriteria(getEntityClass());
		criteria.add(Restrictions.eq("receiptNumber", receiptNumber));

		Bill bill = repository.selectSingle(getEntityClass(), criteria);
		removeNullLineItems(bill);
		return bill;
	}

	@Override
	public List<Bill> findPatientBills(Patient patient, PagingInfo paging) {
		if (patient == null) {
			throw new NullPointerException("The patient must be defined.");
		}

		return findPatientBills(patient.getId(), paging);
	}

	@Override
	public List<Bill> findPatientBills(int patientId, PagingInfo paging) {
		if (patientId < 0) {
			throw new IllegalArgumentException("The patient id must be a valid identifier.");
		}

		Criteria criteria = repository.createCriteria(getEntityClass());
		criteria.add(Restrictions.eq("patient.id", patientId));

		List<Bill> results = repository.select(getEntityClass(), criteria);
		removeNullLineItems(results);
		return results;
	}
	
	@Override
	public List<Bill> findBills(final BillSearch billSearch) {
		return findBills(billSearch, null);
	}
	
	@Override
	public List<Bill> findBills(final BillSearch billSearch, PagingInfo pagingInfo) {
		if (billSearch == null) {
			throw new NullPointerException("The bill search must be defined.");
		} else if (billSearch.getTemplate() == null) {
			throw new NullPointerException("The bill search template must be defined.");
		}

		return executeCriteria(Bill.class, pagingInfo, new Action1<Criteria>() {
			@Override
			public void apply(Criteria criteria) {
				billSearch.updateCriteria(criteria);
				updateLocationUserCriteria(criteria);
			}
		});
	}

	/*
		These methods are overridden to ensure that any null line items (created as part of a bug in 1.7.0) are removed
		from the results before being returned to the caller.
	 */
	@Override
	public List<Bill> getAll(boolean includeVoided, PagingInfo pagingInfo) throws APIException {
		List<Bill> results = super.getAll(includeVoided, pagingInfo);
		removeNullLineItems(results);
		return results;
	}

	@Override
	public Bill getById(int entityId) throws APIException {
		Bill bill = super.getById(entityId);
		removeNullLineItems(bill);
		return bill;
	}

	@Override
	public Bill getByUuid(String uuid) throws APIException {
		Bill bill = super.getByUuid(uuid);
		removeNullLineItems(bill);
		return bill;
	}

	@Override
	public List<Bill> getAll() throws APIException {
		List<Bill> results =super.getAll();
		removeNullLineItems(results);
		return results;
	}

	private void removeNullLineItems(List<Bill> bills) {
		if (bills == null || bills.size() == 0) {
			return;
		}

		for (Bill bill : bills) {
			removeNullLineItems(bill);
		}
	}

	private void removeNullLineItems(Bill bill) {
		if (bill == null) {
			return;
		}

		// Search for any null line items (due to a bug in 1.7.0) and remove them from the line items
		int index = bill.getLineItems().indexOf(null);
		while (index >= 0) {
			bill.getLineItems().remove(index);

			index = bill.getLineItems().indexOf(null);
		}
	}

	@Override
	public String getVoidPrivilege() {
		return CashierPrivilegeConstants.MANAGE_BILLS;
	}

	@Override
	public String getSavePrivilege() {
		return CashierPrivilegeConstants.MANAGE_BILLS;
	}

	@Override
	public String getPurgePrivilege() {
		return CashierPrivilegeConstants.PURGE_BILLS;
	}

	@Override
	public String getGetPrivilege() {
		return CashierPrivilegeConstants.VIEW_BILLS;
	}
	

	private void updateLocationUserCriteria(Criteria criteria) {
	
		User user = Context.getAuthenticatedUser();
		Location location = null;
		
		if (user.hasRole(RoleConstants.SUPERUSER))
			return;
		
		try {
			location = Context.getLocationService().getLocation(Integer.parseInt(user.getUserProperty(LOCATIONPROPERTY)));
		} catch (Exception e) {}
		
		if (location == null) {
			// impossible criterion so that no results will be returned
			criteria.add(Restrictions.isNull("creator"));
			return;
		} 
		
		criteria.add(Restrictions.eq("location", location));		
	}
}

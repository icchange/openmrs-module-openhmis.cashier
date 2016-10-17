package org.openmrs.module.openhmis.cashier.api.impl;

import org.openmrs.Location;
import org.openmrs.OpenmrsObject;
import org.openmrs.Patient;
import org.openmrs.module.openhmis.cashier.api.IBillLineItemService;
import org.openmrs.module.openhmis.cashier.api.model.Bill;
import org.openmrs.module.openhmis.cashier.api.model.BillLineItem;
import org.openmrs.module.openhmis.cashier.api.search.BillSearch;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.commons.api.entity.impl.BaseEntityDataServiceImpl;
import org.openmrs.module.openhmis.commons.api.entity.security.IEntityAuthorizationPrivileges;

import java.util.Collection;
import java.util.List;

/**
 * Created by ICCHANGE on 14/Oct/2016.
 */
public class BillLineItemServiceImpl extends BaseEntityDataServiceImpl<BillLineItem>
        implements IEntityAuthorizationPrivileges
        , IBillLineItemService {
	@Override
	protected IEntityAuthorizationPrivileges getPrivileges() {
		return null;
	}

	@Override
	protected void validate(BillLineItem billLineItem) {

	}

	@Override
	public String getVoidPrivilege() {
		return null;
	}

	@Override
	public String getSavePrivilege() {
		return null;
	}

	@Override
	public String getPurgePrivilege() {
		return null;
	}

	@Override
	public String getGetPrivilege() {
		return null;
	}

	@Override
	public BillLineItem saveOrUpdateLineItem(BillLineItem edit) {
		return super.save(edit);
	}
}

package org.openmrs.module.openhmis.cashier.api;

import org.openmrs.annotation.Authorized;
import org.openmrs.module.openhmis.cashier.api.model.Bill;
import org.openmrs.module.openhmis.cashier.api.model.BillLineItem;
import org.openmrs.module.openhmis.cashier.api.util.PrivilegeConstants;
import org.openmrs.module.openhmis.commons.api.entity.IEntityDataService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by ICCHANGE on 14/Oct/2016.
 */
@Transactional
public interface IBillLineItemService extends IEntityDataService<BillLineItem> {

	@Transactional
	@Authorized({ PrivilegeConstants.ADJUST_BILLS })
	BillLineItem saveOrUpdateLineItem(BillLineItem edit);

}

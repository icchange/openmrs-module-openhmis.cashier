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
package org.openmrs.module.webservices.rest.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openmrs.OpenmrsObject;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.cashier.api.IBillService;
import org.openmrs.module.openhmis.commons.api.entity.IEntityDataService;
import org.openmrs.module.openhmis.cashier.api.ITimesheetService;
import org.openmrs.module.openhmis.cashier.api.model.Bill;
import org.openmrs.module.openhmis.cashier.api.model.BillLineItem;
import org.openmrs.module.openhmis.cashier.api.model.BillStatus;
import org.openmrs.module.openhmis.cashier.api.model.CashPoint;
import org.openmrs.module.openhmis.cashier.api.model.Payment;
import org.openmrs.module.openhmis.cashier.api.model.Timesheet;
import org.openmrs.module.openhmis.cashier.api.util.RoundingUtil;
import org.openmrs.module.openhmis.cashier.web.CashierWebConstants;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.ConversionUtil;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceHandler;
import org.openmrs.module.webservices.rest.web.response.ConversionException;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.validator.ValidateUtil;
import org.springframework.web.client.RestClientException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription.Property;

@Resource(name=RestConstants.VERSION_2 + "/cashier/bill", supportedClass=Bill.class, supportedOpenmrsVersions={"1.9"})
public class BillResource extends BaseRestDataResource<Bill> {
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);
		if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("adjustedBy", Representation.REF);
			description.addProperty("billAdjusted", Representation.REF);
			description.addProperty("cashPoint", Representation.REF);
			description.addProperty("cashier", Representation.REF);
			description.addProperty("lineItems");
			description.addProperty("patient", Representation.REF);
			description.addProperty("payments", Representation.FULL);
			description.addProperty("receiptNumber");
			description.addProperty("status");
		}
		return description;
	}

	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("adjustedBy");
		description.addProperty("billAdjusted");
		description.addProperty("cashPoint");		
		description.addProperty("lineItems");
		description.addProperty("patient");
		description.addProperty("payments");
		description.addProperty("receiptNumber");
		description.addProperty("status");		
		return description;
	}


	public DelegatingResourceDescription getSoftUpdatableProperties() throws ResourceDoesNotSupportOperationException {		
		DelegatingResourceDescription description = super.getUpdatableProperties();
		description.addProperty("cashPoint");
		description.addProperty("cashier");
		description.addProperty("lineItems");
		description.addProperty("payments");
		description.addProperty("status");
		return description;
	}
	
	
	/*
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {		
		DelegatingResourceDescription description = super.getUpdatableProperties();
		description.addProperty("adjustedBy", Representation.REF);
		description.addProperty("billAdjusted", Representation.REF);
		description.addProperty("cashPoint", Representation.REF);
		description.addProperty("cashier", Representation.REF);
		description.addProperty("lineItems");
		description.addProperty("patient", Representation.REF);
		description.addProperty("payments", Representation.FULL);
		description.addProperty("receiptNumber");
		description.addProperty("status");
		return description;
	}
	*/
	
	@Override
	public List<String> getPropertiesToExposeAsSubResources() {
		return Arrays.asList("payments");
	}
	
	@PropertySetter("lineItems")
	public void setBillLineItems(Bill instance, List<BillLineItem> lineItems) {
		if (instance.getLineItems() == null) {
			instance.setLineItems(new ArrayList<BillLineItem>(lineItems.size()));
		}
		BaseRestDataResource.syncCollection(instance.getLineItems(), lineItems);
		for (BillLineItem item: instance.getLineItems()) {
			item.setBill(instance);
		}
	}

	@PropertySetter("payments")
	public void setBillPayments(Bill instance, Set<Payment> payments) {
		if (instance.getPayments() == null) {
			instance.setPayments(new HashSet<Payment>(payments.size()));
		}
		BaseRestDataResource.syncCollection(instance.getPayments(), payments);
		for (Payment payment: instance.getPayments()) {
			instance.addPayment(payment);
		}
	}
	
	@PropertySetter("billAdjusted")
	public void setBillAdjusted(Bill instance, Bill billAdjusted) {
		billAdjusted.addAdjustedBy(instance);
		instance.setBillAdjusted(billAdjusted);
	}
	
	@PropertySetter("status")
	public void setBillStatus(Bill instance, BillStatus status) {
		if (instance.getStatus() == null) {
			instance.setStatus(status);
		} else if (instance.getStatus() == BillStatus.PENDING && status == BillStatus.POSTED) {
			instance.setStatus(status);
		}
		if (status == BillStatus.POSTED) {
			RoundingUtil.addRoundingLineItem(instance);
		}
	}

	@Override
	public Bill save (Bill delegate) {
		//TODO: Test all the ways that this could fail
		if (delegate.getId() == null) {
			if (delegate.getCashier() == null) {
				User currentUser = Context.getAuthenticatedUser();
				ProviderService service = Context.getProviderService();
				Collection<Provider> providers = service.getProvidersByPerson(currentUser.getPerson());
				for (Provider provider : providers) {
					delegate.setCashier(provider);
					break;
				}
				if (delegate.getCashier() == null) {
					throw new RestClientException("Couldn't find Provider for the current user (" + currentUser.getName() + ")");
				}
			}
			if (delegate.getCashPoint() == null) {
				ITimesheetService service = Context.getService(ITimesheetService.class);
				Timesheet timesheet = service.getCurrentTimesheet(delegate.getCashier());
				if (timesheet == null) {
					AdministrationService adminService = Context.getAdministrationService();
					boolean timesheetRequired;
					try {
						timesheetRequired = Boolean.parseBoolean(adminService.getGlobalProperty(CashierWebConstants.TIMESHEET_REQUIRED_PROPERTY));
					} catch (Exception e) {
						timesheetRequired = false;
					}
					if (timesheetRequired) {
						throw new RestClientException("A current timesheet does not exist for cashier " + delegate.getCashier());
					} else if (delegate.getBillAdjusted() != null) {
						// If this is an adjusting bill, copy cash point from billAdjusted
						delegate.setCashPoint(delegate.getBillAdjusted().getCashPoint());
					} else {
						throw new RestClientException("Cash point cannot be null!");
					}
				} else {
					CashPoint cashPoint = timesheet.getCashPoint();
					if (cashPoint == null) {
						throw new RestClientException("No cash points defined for the current timesheet!");
					}
					delegate.setCashPoint(cashPoint);					
				}
			}
			// Now that all all attributes have been set (i.e., payments and
			// bill status) we can check to see if the bill is fully paid.
			delegate.checkPaidAndUpdateStatus();
			if (delegate.getStatus() == null) {
				delegate.setStatus(BillStatus.PENDING);
			}
		}
		return super.save(delegate);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<IEntityDataService<Bill>> getServiceClass() {
		return (Class<IEntityDataService<Bill>>)(Object)IBillService.class;
	}

	public String getDisplayString(Bill instance) {
		return instance.getReceiptNumber();
	}
	
	@Override
	public Bill newDelegate() {
		return new Bill();
	}


	@Override
	public Object update(String uuid, SimpleObject propertiesToUpdate, RequestContext context) throws ResponseException {

		Bill delegate = getByUniqueId(uuid);
			
		
		if (delegate == null)
			throw new ObjectNotFoundException();

		
		if (hasTypesDefined()) {
			// if they specify a type discriminator it must match the expected one--type can't be modified
			if (propertiesToUpdate.containsKey(RestConstants.PROPERTY_FOR_TYPE)) {
				String type = (String) propertiesToUpdate.remove(RestConstants.PROPERTY_FOR_TYPE);
				if (!delegate.getClass().equals(getActualSubclass(type))) {
					String nameToShow = getTypeName(delegate);
					if (nameToShow == null)
						nameToShow = delegate.getClass().getName();
					throw new IllegalArgumentException("You passed " + RestConstants.PROPERTY_FOR_TYPE + "=" + type
					        + " but this instance is a " + nameToShow);
				}
			}
		}

		
		if (delegate.getStatus() == BillStatus.PENDING) {
			
			setConvertedProperties(delegate, propertiesToUpdate, getSoftUpdatableProperties(), false);
			
		} else {
			
			DelegatingResourceHandler<? extends Bill> handler = getResourceHandler(delegate);

			setConvertedProperties(delegate, propertiesToUpdate, handler.getUpdatableProperties(), false);
	
		}
		
		ValidateUtil.validate(delegate);
		delegate = save(delegate);

		SimpleObject ret = (SimpleObject) ConversionUtil.convertToRepresentation(delegate, Representation.DEFAULT);

		// add the 'type' discriminator if we support subclasses
		if (hasTypesDefined()) {
			ret.add(RestConstants.PROPERTY_FOR_TYPE, getTypeName(delegate));
		}

		return ret;		
	}

}

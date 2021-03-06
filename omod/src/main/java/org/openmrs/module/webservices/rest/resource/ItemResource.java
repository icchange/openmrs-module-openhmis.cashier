/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.1 (the "License"); you may not use this file except in
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

import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.cashier.api.IDepartmentService;
import org.openmrs.module.openhmis.cashier.api.IItemService;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.commons.api.entity.IMetadataDataService;
import org.openmrs.module.openhmis.cashier.api.model.Department;
import org.openmrs.module.openhmis.cashier.api.model.Item;
import org.openmrs.module.openhmis.cashier.api.model.ItemCode;
import org.openmrs.module.openhmis.cashier.api.model.ItemPrice;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.PropertySetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.AlreadyPaged;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Resource("item")
@Handler(supports = { Item.class }, order = 0)
public class ItemResource extends BaseRestMetadataResource<Item> {

	@Override
	public SimpleObject search(String query, RequestContext context) throws ResponseException {
		IItemService service = (IItemService) Context.getService(getServiceClass());
		// Try searching by code
		SimpleObject resultByCode = searchByCode(query, context, service);
		if (resultByCode != null) return resultByCode;

		// Do a name search
		PagingInfo pagingInfo = MetadataSearcher.getPagingInfoFromContext(context);
		List<Item> items = service.findByName(query, context.getIncludeAll(), pagingInfo);
		AlreadyPagedWithLength<Item> results = new AlreadyPagedWithLength<Item>(context, items, pagingInfo.hasMoreResults(), pagingInfo.getTotalRecordCount());
		return results.toSimpleObject();
	}
 
	public SimpleObject search(String query, String department_uuid, RequestContext context) throws ResponseException {
		IItemService service = (IItemService) Context.getService(getServiceClass());
		IDepartmentService deptService = (IDepartmentService) Context.getService(IDepartmentService.class);
		Department department = deptService.getByUuid(department_uuid);
		
		// Try searching by code
		SimpleObject resultByCode = searchByCode(query, context, service);
		if (resultByCode != null) return resultByCode;
		
		// Do a name + department search
		PagingInfo pagingInfo = MetadataSearcher.getPagingInfoFromContext(context);
		List<Item> items = service.findItems(department, query, context.getIncludeAll(), pagingInfo);
		PageableResult results = new AlreadyPagedWithLength<Item>(context, items, pagingInfo.hasMoreResults(), pagingInfo.getTotalRecordCount());
		return results.toSimpleObject();
	}
	
	protected SimpleObject searchByCode(String query, RequestContext context, IItemService service) throws ResponseException {
		if (service == null) service = (IItemService) Context.getService(getServiceClass());
		Item itemByCode = service.getItemByCode(query);
		if (itemByCode != null) {
			List<Item> list = new ArrayList<Item>(1);
			list.add(itemByCode);
			return new AlreadyPaged<Item>(context, list, false).toSimpleObject();
		}
		return null;
	}
	
	public SimpleObject searchByDepartment(String department_uuid, RequestContext context) throws ResponseException {
		IItemService service = (IItemService) Context.getService(getServiceClass());
		IDepartmentService deptService = (IDepartmentService) Context.getService(IDepartmentService.class);
		Department department = deptService.getByUuid(department_uuid);
		
		PagingInfo pagingInfo = MetadataSearcher.getPagingInfoFromContext(context);
		List<Item> items = service.getItemsByDepartment(department, context.getIncludeAll(), pagingInfo);
		PageableResult results = new AlreadyPagedWithLength<Item>(context, items, pagingInfo.hasMoreResults(), pagingInfo.getTotalRecordCount());
		return results.toSimpleObject();
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(
			Representation rep) {
		DelegatingResourceDescription description = super.getRepresentationDescription(rep);
		if (rep instanceof RefRepresentation) {
			description.addProperty("codes", Representation.REF);
			description.addProperty("department", Representation.REF);
			description.addProperty("defaultPrice", Representation.REF);
		}
		else if (rep instanceof DefaultRepresentation || rep instanceof FullRepresentation) {
			description.addProperty("name");
			description.addProperty("codes", Representation.REF);
			description.addProperty("prices", Representation.REF);
			description.addProperty("department", Representation.REF);
			description.addProperty("defaultPrice", Representation.REF);
		}
		return description;
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = super.getCreatableProperties();
		description.addProperty("name");
		description.addProperty("codes");
		description.addProperty("prices");
		description.addProperty("department");
		description.addProperty("defaultPrice");
		return description;
	}	

	@PropertySetter(value="codes")
	public void setItemCodes(Item instance, Set<ItemCode> codes) {
		if (instance.getCodes() == null)
			instance.setCodes(new HashSet<ItemCode>());
		BaseRestDataResource.updateCollection(instance.getCodes(), codes);
		for (ItemCode code : instance.getCodes()) {
			code.setItem(instance);
		}
	}

	@PropertySetter(value="prices")
	public void setItemPrices(Item instance, Set<ItemPrice> prices) {
		if (instance.getPrices() == null)
			instance.setPrices(new HashSet<ItemPrice>());
		BaseRestDataResource.updateCollection(instance.getPrices(), prices);
		for (ItemPrice price : instance.getPrices()) {
			price.setItem(instance);
		}
	}
	
	/**
	 * Set the default price.
	 * 
	 * Typically will use a uuid, but in the case of creating a new price (not
	 * yet having a uuid), we compare price strings.  Dubious? 
	 * 
	 * @param instance
	 * @param uuidOrPrice
	 */
	@PropertySetter(value="defaultPrice")
	public void setDefaultPrice(Item instance, String uuidOrPrice) {
		for (ItemPrice price : instance.getPrices()) {
			if (price.getUuid().equals(uuidOrPrice)) {
				instance.setDefaultPrice(price);
				return;
			}
			else if (price.getPrice().toPlainString().equals(uuidOrPrice)) {
				instance.setDefaultPrice(price);
				return;
			}
		}
		// If there are no matches in the current price set, save the price in
		// a new ItemPrice to hopefully be updated later, in case we haven't
		// set new prices yet.
		ItemPrice defaultPrice = new ItemPrice(new BigDecimal(uuidOrPrice), "");
		instance.setDefaultPrice(defaultPrice);
	}
	
	@Override
	public Item save(Item delegate) {
		// Check that default price has been properly set now that the item's
		// prices have definitely been set
		if (!delegate.getPrices().contains(delegate.getDefaultPrice())) {
			if (delegate.getDefaultPrice().getId() == null)
				setDefaultPrice(delegate, delegate.getDefaultPrice().getPrice().toString());
			// If it's still not set to one of the item's prices, set it to the
			// first available price, or null.
			if (!delegate.getPrices().contains(delegate.getDefaultPrice())) {
				if (delegate.getPrices().size() > 0)
					delegate.setDefaultPrice(delegate.getPrices().toArray(new ItemPrice[0])[0]);
				else
					delegate.setDefaultPrice(null);
			}
		}
		return super.save(delegate);
	}

	@Override
	public Item newDelegate() {
		return new Item();
	}
	
	@Override
	public Class<? extends IMetadataDataService<Item>> getServiceClass() {
		return IItemService.class;
	}
}

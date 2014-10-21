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
package org.openmrs.module.openhmis.cashier.web.controller;

import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.cashier.api.IBillService;
import org.openmrs.module.openhmis.cashier.api.model.Bill;
import org.openmrs.module.openhmis.cashier.api.model.Timesheet;
import org.openmrs.module.openhmis.cashier.api.util.CashierPrivilegeConstants;
import org.openmrs.module.openhmis.cashier.api.util.TimesheetHelper;
import org.openmrs.module.openhmis.cashier.api.util.TimesheetRequiredException;
import org.openmrs.module.openhmis.cashier.web.CashierWebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.List;
import java.lang.Integer;
import java.lang.String;
import org.directwebremoting.util.Logger;

import org.openmrs.User;

@Controller
@RequestMapping(value = CashierWebConstants.BILL_PAGE)
public class BillAddEditController {
	
	private static final Logger LOG = Logger.getLogger(BillAddEditController.class);
	
	@RequestMapping(method = RequestMethod.GET)
	public String bill(ModelMap model, 
			@RequestParam(value = "billUuid", required = false) String billUuid,
			@RequestParam(value = "patientUuid", required = false) String patientUuid,
			@RequestParam(value = "patientId", required = false) String patientId,
			//@RequestParam(value = "patient", required = false) Patient patient,
			HttpServletRequest request) throws UnsupportedEncodingException 
	{
		Patient patient = null;
		Timesheet timesheet = null;
		
		//LOG.info("In billAddEdit controller");
		
		try 
		{
			timesheet = TimesheetHelper.getCurrentTimesheet(); 
		}
		catch (TimesheetRequiredException e) 
		{
			return "redirect:/" + CashierWebConstants.formUrl(CashierWebConstants.CASHIER_PAGE)
				+ "?returnUrl=" + CashierWebConstants.formUrl(CashierWebConstants.BILL_PAGE)
				+ (request.getQueryString() != null ? UriUtils.encodeQuery("?" + request.getQueryString(), "UTF-8") : "");
		} 
		catch (Exception e) 
		{
			// Catch other exceptions, like session timeout
		}
		

		model.addAttribute("timesheet", timesheet);
		User user = Context.getAuthenticatedUser();
		model.addAttribute("user", user);
		int location_id = Integer.parseInt(user.getUserProperty(LOCATIONPROPERTY));
		'model.addAttribute("location_id", location_id);
		model.addAttribute("url", CashierWebConstants.formUrl(CashierWebConstants.BILL_PAGE) + ( (request.getQueryString() != null) ? "?" + request.getQueryString() : ""));
		
		if (billUuid != null) 
		{
			//Getting bill if billUuid already provided
			LOG.info("In billAddEdit controller, looking up bills by billUuid: " + billUuid);
			Bill bill = null;
			IBillService service = Context.getService(IBillService.class);
			bill = service.getByUuid(billUuid);
			patient = bill.getPatient();
			model.addAttribute("bill", bill);
			model.addAttribute("billAdjusted", bill.getBillAdjusted());
			model.addAttribute("adjustedBy", bill.getAdjustedBy());
			model.addAttribute("patient", patient);
			{
				LOG.info("patient found, retrieving bills");
				List<Bill> bills = service.findPatientBills(patient, null);
				model.addAttribute("bills", bills);
			}
			model.addAttribute("patientInfo", patient);
			model.addAttribute("cashPoint", bill.getCashPoint());
			if (!bill.isReceiptPrinted() || (bill.isReceiptPrinted() && Context.hasPrivilege(CashierPrivilegeConstants.REPRINT_RECEIPT))) 
			{
				model.addAttribute("showPrint", true);
			}
			return CashierWebConstants.BILL_PAGE;
		}
		else 
		{
			IBillService billService = Context.getService(IBillService.class);
			
			if (patientUuid != null) 
			{
				LOG.info("In billAddEdit controller, looking up patient by patientUuid: " + patientUuid);
				String patientIdentifier = null;
				PatientService service = Context.getPatientService();
				patient = service.getPatientByUuid(patientUuid);
				Set<PatientIdentifier> identifiers = patient.getIdentifiers();
				for (PatientIdentifier id : identifiers) 
				{
					if (id.getPreferred()) 
					{
						patientIdentifier = id.getIdentifier();
					}
				}
				model.addAttribute("patient", patient);
				{
					LOG.info("patient found, retrieving bills");
					List<Bill> bills = billService.findPatientBills(patient, null);
					model.addAttribute("bills", bills);
				}
				model.addAttribute("patientIdentifier", patientIdentifier);
				model.addAttribute("cashPoint", timesheet != null ? timesheet.getCashPoint() : null);
				return CashierWebConstants.BILL_PAGE;
			}
			else
				if ((patientId != null))
				{
					LOG.info("In billAddEdit controller, looking up patient by patientId: " + patientId);
					String patientIdentifier = null;
					PatientService service = Context.getPatientService();
					patient = service.getPatient(Integer.parseInt(patientId));
					LOG.info("In billAddEdit controller, found patient");
					Set<PatientIdentifier> identifiers = patient.getIdentifiers();
					for (PatientIdentifier id : identifiers) 
					{
						if (id.getPreferred()) 
						{
							patientIdentifier = id.getIdentifier();
						}
					}
					model.addAttribute("patient", patient);
					{
						LOG.info("patient found, retrieving bills");
						List<Bill> bills = billService.findPatientBills(patient, null);
						model.addAttribute("bills", bills);
					}
					model.addAttribute("patientInfo", patient);
					model.addAttribute("patientIdentifier", patientIdentifier);
					model.addAttribute("cashPoint", timesheet != null ? timesheet.getCashPoint() : null);
					return CashierWebConstants.redirectUrl(CashierWebConstants.BILL_PAGE) + "?" + "&patientUuid=" + patient.getUuid();// + "&patientId=" + patient.getPatientId().toString();
				}
			//This button is commented out because our users do not have printers
			//model.addAttribute("showPrint", true);
			model.addAttribute("cashPoint", timesheet != null ? timesheet.getCashPoint() : null);
			return CashierWebConstants.BILL_PAGE;
		}
	}
}
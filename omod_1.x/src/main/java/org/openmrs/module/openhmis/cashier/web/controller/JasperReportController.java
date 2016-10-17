/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and
 * limitations under the License.
 *
 * Copyright (C) OpenHMIS.  All Rights Reserved.
 */
package org.openmrs.module.openhmis.cashier.web.controller;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.jasperreport.ReportsControllerBase;
import org.openmrs.module.openhmis.cashier.ModuleSettings;
import org.openmrs.module.openhmis.cashier.api.model.CashierSettings;
import org.openmrs.module.openhmis.cashier.web.CashierWebConstants;
import org.openmrs.module.openhmis.inventory.api.model.Settings;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

/**
 * Controller to manage the Jasper Reports page.
 */
@Controller
@RequestMapping(value = CashierWebConstants.JASPER_REPORT_PAGE)
public class JasperReportController extends ReportsControllerBase {

	private static final Log LOG = LogFactory.getLog(CashierController.class);

	@Override
	public String parse(int reportId, WebRequest request, HttpServletResponse response) throws IOException {
		CashierSettings settings = org.openmrs.module.openhmis.cashier.ModuleSettings.loadSettings();
		HashMap<String, Object> params = new HashMap<String, Object>();

		if (settings.getDefaultShitReportId() != null
		        && reportId == settings.getDefaultShitReportId().intValue()) {

			int timesheetId;
			String temp = request.getParameter("timesheetId");
			if (!StringUtils.isEmpty(temp) && StringUtils.isNumeric(temp)) {
				timesheetId = Integer.parseInt(temp);
			} else {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The timesheet id ('" + temp
				        + "') must be "
				        + "defined and be numeric.");
				return null;
			}
			params.put("timesheetId", timesheetId);
			return renderReport(reportId, params, "Cashier Shift Report - " + temp, response);

		} else if (settings.getDefaultRevenueReportId() != null
		        && reportId == settings.getDefaultRevenueReportId().intValue()) {

			try {
				SimpleDateFormat dateformat = new SimpleDateFormat("MM/dd/yyyy HH:mm");
				String startdate = request.getParameter("revenueDateStart");
				Date sdate = dateformat.parse(startdate);
				params.put("beginDate", sdate);
				String enddate = request.getParameter("revenueDateEnd");
				Date edate = dateformat.parse(enddate);
				params.put("endDate", edate);
				String loc =
				        Context.getAuthenticatedUser().getUserProperty(OpenmrsConstants.USER_PROPERTY_DEFAULT_LOCATION);
				Location ltemp = Context.getLocationService().getLocation(Integer.parseInt(loc));
				params.put("locationId", loc);
				params.put("locationName", ltemp.getName());
				return renderReport(reportId, params, "Cashier Revenue Report", response);
			} catch (Exception ex) {
				LOG.error("Error while procession revenue report: bad date format", ex);
			}

		}
		return null;
	}
}

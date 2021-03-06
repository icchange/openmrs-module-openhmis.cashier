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
package org.openmrs.module.openhmis.cashier.api.model;

import org.openmrs.BaseOpenmrsMetadata;

/**
 * Model class that represents an institutional department.
 */
public class Department extends BaseOpenmrsMetadata {
	private Integer departmentId;

	@Override
	public Integer getId() {
		return departmentId;
	}

	@Override
	public void setId(Integer id) {
		departmentId = id;
	}
}

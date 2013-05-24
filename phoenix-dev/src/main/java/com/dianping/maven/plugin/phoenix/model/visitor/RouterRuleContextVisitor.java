package com.dianping.maven.plugin.phoenix.model.visitor;

import com.dianping.maven.plugin.phoenix.F5Manager;
import com.dianping.maven.plugin.phoenix.F5Pool;
import com.dianping.maven.plugin.phoenix.RouterRuleContext;
import com.dianping.maven.plugin.phoenix.model.entity.BizProject;
import com.dianping.maven.plugin.phoenix.model.entity.PhoenixProject;

public class RouterRuleContextVisitor extends AbstractVisitor<RouterRuleContext> {

	private F5Manager f5Mgr;

	public RouterRuleContextVisitor(F5Manager f5Mgr) {
		result = new RouterRuleContext();
		this.f5Mgr = f5Mgr;
	}

	@Override
	public void visitBizProject(BizProject bizProject) {
		F5Pool pool = f5Mgr.poolForProject(bizProject.getName());
		if (pool != null) {
			// web project
			result.addLocalPool(pool);
		}
		super.visitBizProject(bizProject);
	}

	@Override
	public void visitPhoenixProject(PhoenixProject phoenixProject) {
		result.setDefaultUrlPattern(phoenixProject.getRouter().getDefaultUrlPattern());
		super.visitPhoenixProject(phoenixProject);
	}

}
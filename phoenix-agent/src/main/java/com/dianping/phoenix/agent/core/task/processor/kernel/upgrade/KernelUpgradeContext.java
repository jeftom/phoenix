package com.dianping.phoenix.agent.core.task.processor.kernel.upgrade;

import org.unidal.lookup.annotation.Inject;

import com.dianping.cat.Cat;
import com.dianping.phoenix.agent.core.shell.ScriptExecutor;
import com.dianping.phoenix.agent.core.task.Task;
import com.dianping.phoenix.agent.core.task.processor.kernel.DeployTask;
import com.dianping.phoenix.agent.core.task.workflow.Context;

public class KernelUpgradeContext extends Context {
	@Inject
	private ScriptExecutor scriptExecutor;
	@Inject
	private KernelUpgradeStepProvider stepProvider;

	private com.dianping.cat.message.Transaction c_kernelUpgrade;

	public ScriptExecutor getScriptExecutor() {
		return scriptExecutor;
	}

	public KernelUpgradeStepProvider getStepProvider() {
		return stepProvider;
	}

	public com.dianping.cat.message.Transaction getCatTransaction() {
		return c_kernelUpgrade;
	}

	@Override
	public boolean kill() {
		try {
			setKilled(true);
			scriptExecutor.kill();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void setTask(Task task) {
		super.setTask(task);
		DeployTask tsk = (DeployTask) task;
		c_kernelUpgrade = Cat.getProducer().newTransaction("Kernel",
				String.format("%s::%s", tsk.getDomain(), tsk.getKernelVersion()));
		try {
			setMsgId(Cat.getProducer().createMessageId());
		} catch (Exception e) {
			setMsgId("no-cat-id");
		}
	}
}

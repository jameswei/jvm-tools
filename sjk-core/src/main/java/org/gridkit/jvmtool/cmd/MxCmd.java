package org.gridkit.jvmtool.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.jvmtool.JmxConnectionInfo;
import org.gridkit.jvmtool.MBeanHelper;
import org.gridkit.jvmtool.SJK;
import org.gridkit.jvmtool.SJK.CmdRef;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParametersDelegate;

public class MxCmd implements CmdRef {

	@Override
	public String getCommandName() {
		return "mx";
	}

	@Override
	public Runnable newCommand(SJK host) {
		return new MX(host);
	}

	public static class MX implements Runnable {
		
		@SuppressWarnings("unused")
		@ParametersDelegate
		private SJK host;
		
		@ParametersDelegate
		private JmxConnectionInfo connInfo = new JmxConnectionInfo();

		@Parameter(names={"-b", "--bean"}, required = true, description="MBean name")
		private String mbean;
		
		@Parameter(names={"-f", "--field", "--attribute"}, description="MBean attribute")
		String attrib = null;
		
		@ParametersDelegate
		private CallCmd call = new CallCmd();
		
		@ParametersDelegate
		private GetCmd get = new GetCmd();
		
		@ParametersDelegate
		private SetCmd set = new SetCmd();
		
		@ParametersDelegate
		private InfoCmd info = new InfoCmd();
		
		public MX(SJK host) {
			this.host = host;
		}
		
		@Override
		public void run() {
			try {
				List<Runnable> action = new ArrayList<Runnable>();
				if (call.run) {
					action.add(call);
				}
				if (get.run) {
					action.add(get);
				}
				if (set.run) {
					action.add(set);
				}
				if (info.run) {
					action.add(info);
				}
				if (action.isEmpty() || action.size() > 1) {
					SJK.failAndPrintUsage("You should choose one of --info, --get, --set, --call");
				}
				action.get(0).run();
			} catch (Exception e) {
				SJK.fail(e.toString());
			}
		}

		private ObjectName resolveSingleBean(MBeanServerConnection conn) throws Exception {
			ObjectName name = new ObjectName(mbean);
			Set<ObjectName> beans = conn.queryNames(name, null);
			if (beans.isEmpty()) {
				SJK.fail("MBean not found: " + mbean);
			}
			if (beans.size() > 1) {
				StringBuilder sb = new StringBuilder();
				for(ObjectName n: beans) {
					sb.append('\n').append(n);
				}
				SJK.fail("Ambigous MBean selection" + sb.toString());
			}
			return beans.iterator().next();
		}

		class CallCmd implements Runnable {
			
			@Parameter(names={"-mc", "--call"}, description="Invokes MBean method")
			boolean run;
			
			@Parameter(names={"-op", "--operation"}, description="MBean operation name to be called")
			String operation = null;
			
			@Parameter(names={"-a", "--arguments"}, variableArity=true, description="Arguments for MBean operation invocation")
			List<String> arguments = new ArrayList<String>();

			@Override
			public void run() {
				try {
					if (operation == null) {
						SJK.failAndPrintUsage("MBean operation name is missing");
					}
					MBeanServerConnection conn = connInfo.getMServer();
					ObjectName name = resolveSingleBean(conn);
					MBeanHelper helper = new MBeanHelper(conn);
					System.out.println(helper.invoke(name, operation, arguments.toArray(new String[0])));
					
				} catch (Exception e) {
					e.printStackTrace();
					SJK.fail();
				}
			}
		}
		
		class GetCmd implements Runnable {
			
			@Parameter(names={"-mg", "--get"}, description="Retrieves value of MBean attribute")
			boolean run;
			
			@Override
			public void run() {
				try {
					if (attrib == null) {
						SJK.failAndPrintUsage("MBean operation name is missing");
					}
					MBeanServerConnection conn = connInfo.getMServer();
					ObjectName name = resolveSingleBean(conn);
					MBeanHelper helper = new MBeanHelper(conn);
					System.out.println(helper.get(name, attrib));
					
				} catch (Exception e) {
					SJK.fail(e.toString());
				}
			}
		}
		
		class SetCmd implements Runnable {
		
			@Parameter(names={"-ms", "--set"}, description="Sets value for MBean attribute")
			boolean run;
			
			@Parameter(names={"-v", "--value"}, description="Value to set to attribute")
			String value = null;			
			
			@Override
			public void run() {
				try {
					if (attrib == null) {
						SJK.failAndPrintUsage("MBean attribute name is missing");
					}
					if (value == null) {
						SJK.failAndPrintUsage("Value is required");
					}
					MBeanServerConnection conn = connInfo.getMServer();
					ObjectName name = resolveSingleBean(conn);
					MBeanHelper helper = new MBeanHelper(conn);
					helper.set(name, attrib, value);
				} catch (Exception e) {
					SJK.fail(e.toString());
				}
			}
		}
		
		class InfoCmd implements Runnable {
			
			@Parameter(names={"-mi", "--info"}, description="Display metadata for MBean")
			boolean run;
			
			@Override
			public void run() {
				try {
					MBeanServerConnection conn = connInfo.getMServer();
					ObjectName name = resolveSingleBean(conn);
					MBeanHelper helper = new MBeanHelper(conn);
					System.out.println(helper.describe(name));
				} catch (Exception e) {
					SJK.fail(e.toString());
				}
			}
		}
	}	
}

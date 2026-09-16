import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
public class OwnershipTestLauncher {
 public static void main(String[] args) {
  var listener=new SummaryGeneratingListener(); var launcher=LauncherFactory.create();
  launcher.registerTestExecutionListeners(listener);
  var request=LauncherDiscoveryRequestBuilder.request();
  if(args.length==1) request.selectors(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(args[0]));
  else request.selectors(selectPackage("com.ym.iot.ownership"),selectPackage("com.ym.system.ownership"))
    .filters(org.junit.platform.engine.discovery.ClassNameFilter.excludeClassNamePatterns(".*DeviceOwnershipMySqlLockTest"));
  launcher.execute(request.build());
  listener.getSummary().printTo(new java.io.PrintWriter(System.out));
  listener.getSummary().printFailuresTo(new java.io.PrintWriter(System.out));
  if(listener.getSummary().getTestsFoundCount()==0 || listener.getSummary().getTotalFailureCount()!=0) System.exit(1);
 }
}

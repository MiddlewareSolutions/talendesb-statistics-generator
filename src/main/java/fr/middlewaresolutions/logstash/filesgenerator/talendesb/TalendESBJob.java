package fr.middlewaresolutions.logstash.filesgenerator.talendesb;

import java.util.Calendar;
import java.util.ResourceBundle;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import fr.middlewaresolutions.metricbeat.filesgenerator.AbstractClient;

@DisallowConcurrentExecution
public class TalendESBJob extends AbstractClient implements Job {

	/** Properties for talend ESB */
	private static ResourceBundle rbTemplates = ResourceBundle.getBundle("talendesb");
	
	/** Pattern to find routes */
	private String camelRoutePattern = rbTemplates.getString("pattern");
	
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		JobDataMap dataMap = context.getJobDetail().getJobDataMap();
		
		LOG.info("Start with parameters: "+dataMap);
		
		// TODO Auto-generated method stub
		start(
			(String) dataMap.get("url"),
			(String) dataMap.get("user"),
			(String) dataMap.get("passwd"),
			(String) dataMap.get("path")
		);
		
	}

	/**
	 * start processing
	 * 
	 */
	private void start(String url, String user, String pwd, String path) {
		 try {
			// ref time
    		Calendar ref = Calendar.getInstance();
    		String date = sdf.format(ref.getTime());
	    		
			 // connect JVM
    		connectToJVM(url, user, pwd);
    		
    		StringBuffer mbContent = new StringBuffer();
    		
    		// generate a file
	    	String fileName = rbTemplates.getString("prefix")+sdfFile.format(ref.getTime());
	    	fileName = path+fileName+".cvs";
	    	
	    	// generate header if file not existing
    		if (!fileExist(fileName)) {
	    		mbContent.append("timestamp");
				mbContent.append(";");
				
	    		// Add headers
	    		for(String attributeName: rbTemplates.getString("mbean.attributes").split("\n")) {	
	    			mbContent.append(attributeName);
	    			mbContent.append(";");
	    		}
	    		mbContent.append("\n");
    		}
    		
    		
    		
    		// Generate lines
    		for(ObjectInstance oi: listMBeans(camelRoutePattern)) {
    			mbContent.append(date);
    			mbContent.append(";");
    			
    			// add line
    			mbContent.append(mBean2CSV(oi));
    			
    			// end of line
    			mbContent.append("\n");
    			
    			msc.invoke(oi.getObjectName(), "reset", null, null);
    		}
	    	            			
	    	// for this date, generate file
	    	generateFile(fileName, mbContent);
    		

        } catch (Exception e) {
            LOG.warning("Error during TalendESB collect. "+e.getMessage());
        } finally {
        	disconnectToJVM();
		}
		 
		 
	}
	
	
	
	/**
	 * Generate content for one Object Instance
	 * @param oi
	 * @return
	 * @throws Exception 
	 */
	private StringBuffer mBean2CSV(ObjectInstance oi) 
	throws Exception {
		StringBuffer mbContent = new StringBuffer();
		
		ObjectName route = oi.getObjectName();
		String name = route.getCanonicalName(); 
        
		// get attributes
		AttributeList  attrList = msc.getAttributes(route, 
				rbTemplates.getString("mbean.attributes").split("\n"));
		
		for(Attribute attribute: attrList.asList()) {	
			mbContent.append(attribute.getValue());
			mbContent.append(";");
		}
		
		return mbContent;
	}
}

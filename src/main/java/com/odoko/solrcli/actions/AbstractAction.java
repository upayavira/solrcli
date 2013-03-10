package com.odoko.solrcli.actions;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;


public abstract class AbstractAction implements Action {

  protected static final String DEFAULT_SOLR_URL = "http://localhost:8983/solr";
  protected List<String> arguments;
  protected Map<String, String> options;
  protected String solrUrl = DEFAULT_SOLR_URL;
  protected boolean quiet = false;
  
  @Override
  public void configure(List<String> arguments, Map<String, String> options) {
    this.arguments = arguments;
    this.options = options;
  }

  protected boolean isTrue(String str) {
    return isTrue(str, true);
  }
  
  protected boolean isTrue(String str, boolean nullMeans) {
	  if (str == null) {
		  return nullMeans;
	  }
	  str = str.toLowerCase().trim();
	  return str.equals("true") || str.equals("yes") || str.equals("on") || str.equals("1");
  }
	 
  protected void warn(String msg) {
	  if (!quiet) {
        System.err.println("SimplePostTool: WARNING: " + msg);
	  }
  }

  protected void info(String msg) {
	  if (!quiet) {
        System.out.println(msg);
	  }
  }

  protected void fatal(String msg) {
    System.err.println("SimplePostTool: FATAL: " + msg);
    System.exit(2);
  }

  /**
   * Pretty prints the number of milliseconds taken to post the content to Solr
   * @param millis the time in milliseconds
   */
  protected static void displayTiming(long millis) {
    SimpleDateFormat df = new SimpleDateFormat("H:mm:ss.SSS", Locale.getDefault());
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    System.out.println("Time spent: "+df.format(new Date(millis)));
  }

  protected SolrServer getSolrServer() {
	  return new HttpSolrServer(solrUrl);
  }
  
  protected URL getSolrURL() {
	  try {
		  return new URL(solrUrl);
	  } catch (MalformedURLException e) {
		  throw new RuntimeException("Invalid URL: " + solrUrl);
	  }
  }
	  
  protected boolean checkParam(String name, String value) {
	if (name.equals("url")) {
		solrUrl = value;
	} else if (name.equals("quiet")) {
		quiet = true;
	} else {
		return false;
	}
	return true;
  }
  
  /**
   * Appends to the path of the URL
   * @param url the URL
   * @param append the path to append
   * @return the final URL version 
   */
  protected URL appendUrlPath(URL url, String append) throws MalformedURLException {
    return new URL(url.getProtocol() + "://" + url.getAuthority() + url.getPath() + append + (url.getQuery() != null ? "?"+url.getQuery() : ""));
  }
  
  /**
   * Appends a URL query parameter to a URL 
   * @param url the original URL
   * @param param the parameter(s) to append, separated by "&"
   * @return the string version of the resulting URL
   */
  protected String appendParam(String url, String param) {
    String[] pa = param.split("&");
    for(String p : pa) {
      if(p.trim().length() == 0) continue;
      String[] kv = p.split("=");
      if(kv.length == 2) {
        url = url + (url.indexOf('?')>0 ? "&" : "?") + kv[0] +"="+ kv[1];
      } else {
        warn("Skipping param "+p+" which is not on form key=value");
      }
    }
    return url;
  }

}

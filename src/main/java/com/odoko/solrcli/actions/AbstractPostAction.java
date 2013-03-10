package com.odoko.solrcli.actions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public abstract class AbstractPostAction extends AbstractAction {

  private static final String DEFAULT_CONTENT_TYPE = "application/xml";
  private static final String DEFAULT_SOLR_UPDATE_URL = "http://localhost:8983/solr/update";

  protected boolean commit;
  protected boolean optimize;
  protected String solrUpdateUrl = DEFAULT_SOLR_UPDATE_URL;

  /**
   * Does a simple commit operation 
   */
  public void commit() {
	if (commit) {
	    info("COMMITting Solr index changes to " + solrUrl + "..");
	    doGet(appendParam(solrUrl.toString(), "commit=true"));
	}
  }

  /**
   * Does a simple optimize operation 
   */
  public void optimize() {
	  if (optimize) {
	    info("Performing an OPTIMIZE to " + solrUrl + "..");
	    doGet(appendParam(solrUrl.toString(), "optimize=true"));
	  }
  }

  /**
   * Performs a simple get on the given URL
   */
  protected void doGet(String url) {
    try {
      doGet(new URL(url));
    } catch (MalformedURLException e) {
      warn("The specified URL "+url+" is not a valid URL. Please check");
    }
  }
  
  /**
   * Performs a simple get on the given URL
   */
  protected void doGet(URL url) {
    try {
      HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
      if (HttpURLConnection.HTTP_OK != urlc.getResponseCode()) {
        warn("Solr returned an error #" + urlc.getResponseCode() + 
            " " + urlc.getResponseMessage() + " for url "+url);
      }
    } catch (IOException e) {
      warn("An error occurred posting data to "+url+". Please check that Solr is running.");
    }
  }

  public boolean checkParam(String name, String value) {
	if (name.equals("url")) {
		solrUpdateUrl = value;
	} else if (super.checkParam(name, value)) {
		return true;
	} else if (name.equals("commit")) {
		commit = true;
	} else if (name.equals("optimize")) {
		optimize = true;
	} else {
		return false;
	}
	return true;
  }
  /**
   * Reads data from the data stream and posts it to solr,
   * writes to the response to output
   * @return true if success
   */
  public boolean postData(InputStream data, Integer length, OutputStream output, String type, URL url) {
    boolean success = true;
    if(type == null)
      type = DEFAULT_CONTENT_TYPE;
    HttpURLConnection urlc = null;
    try {
      try {
        urlc = (HttpURLConnection) url.openConnection();
        try {
          urlc.setRequestMethod("POST");
        } catch (ProtocolException e) {
          fatal("Shouldn't happen: HttpURLConnection doesn't support POST??"+e);
        }
        urlc.setDoOutput(true);
        urlc.setDoInput(true);
        urlc.setUseCaches(false);
        urlc.setAllowUserInteraction(false);
        urlc.setRequestProperty("Content-type", type);

        if (null != length) urlc.setFixedLengthStreamingMode(length);

      } catch (IOException e) {
        fatal("Connection error (is Solr running at " + solrUrl + " ?): " + e);
        success = false;
      }
      
      OutputStream out = null;
      try {
        out = urlc.getOutputStream();
        pipe(data, out);
      } catch (IOException e) {
        fatal("IOException while posting data: " + e);
        success = false;
      } finally {
        try { if(out!=null) out.close(); } catch (IOException x) { /*NOOP*/ }
      }
      
      InputStream in = null;
      try {
        if (HttpURLConnection.HTTP_OK != urlc.getResponseCode()) {
          warn("Solr returned an error #" + urlc.getResponseCode() + 
                " " + urlc.getResponseMessage());
          success = false;
        }

        in = urlc.getInputStream();
        pipe(in, output);
      } catch (IOException e) {
        warn("IOException while reading response: " + e);
        success = false;
      } finally {
        try { if(in!=null) in.close(); } catch (IOException x) { /*NOOP*/ }
      }
      
    } finally {
      if(urlc!=null) urlc.disconnect();
    }
    return success;
  }


  /**
   * Pipes everything from the source to the dest.  If dest is null, 
   * then everything is read from source and thrown away.
   */
  private static void pipe(InputStream source, OutputStream dest) throws IOException {
    byte[] buf = new byte[1024];
    int read = 0;
    while ( (read = source.read(buf) ) >= 0) {
      if (null != dest) dest.write(buf, 0, read);
    }
    if (null != dest) dest.flush();
  }

  protected URL getSolrUpdateURL() {
	  try {
		  return new URL(solrUpdateUrl);
	  } catch (MalformedURLException e) {
		  throw new RuntimeException("Invalid URL: " + solrUrl);
	  }
  }
}

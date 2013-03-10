  /*
   * Licensed to the Apache Software Foundation (ASF) under one or more
   * contributor license agreements.  See the NOTICE file distributed with
   * this work for additional information regarding copyright ownership.
   * The ASF licenses this file to You under the Apache License, Version 2.0
   * (the "License"); you may not use this file except in compliance with
   * the License.  You may obtain a copy of the License at
   *
   *     http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
package com.odoko.solrcli.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class CrawlPostAction extends AbstractPostAction {

  private boolean commit = true;
  private boolean optimize = false;
  private String querystring = "*:*";
  private boolean omitHeader = false;
  private List<String> fieldlist = new ArrayList<String>();
  private Map<String,String> params = new HashMap<String, String>();
  
  @Override
  public void init() {
    if (arguments.size() > 0) {
      querystring = arguments.get(0);
    }
    for (Entry<String, String> option : options.entrySet()) {
      String key = option.getKey();
      String value = option.getValue();
      if (key.equals("omitHeader")) {
        if (value == null) {
          omitHeader = true;
        } else {
            omitHeader=parseBoolean(value);
        }
      } else if (key.equals("fl")) {
        fieldlist = parseFieldList(value);
      } else {
        params.put(key,  value);
      }
    }
    
    String urlStr = null;
    try {
      // Parse args
      String params = System.getProperty("params", "");
      urlStr = System.getProperty("url", DEFAULT_POST_URL);
      urlStr = appendParam(urlStr, params);
      URL url = new URL(urlStr);
      boolean auto = isOn(System.getProperty("auto", DEFAULT_AUTO));
      String type = System.getProperty("type");
      // Recursive
      int recursive = 0;
      String r = System.getProperty("recursive", DEFAULT_RECURSIVE);
      try {
        recursive = Integer.parseInt(r);
      } catch(Exception e) {
        if (isOn(r))
          recursive = 1;
      }
      // Delay
      int delay = DATA_MODE_WEB.equals(mode) ? DEFAULT_WEB_DELAY : 0;
      try {
        delay = Integer.parseInt(System.getProperty("delay", ""+delay));
      } catch(Exception e) { }
      OutputStream out = isOn(System.getProperty("out", DEFAULT_OUT)) ? System.out : null;
      String fileTypes = System.getProperty("filetypes", DEFAULT_FILE_TYPES);
      boolean commit = isOn(System.getProperty("commit",DEFAULT_COMMIT));
      boolean optimize = isOn(System.getProperty("optimize",DEFAULT_OPTIMIZE));
      
      return new SimplePostTool(mode, url, auto, type, recursive, delay, fileTypes, out, commit, optimize, args);
    } catch (MalformedURLException e) {
      fatal("System Property 'url' is not a valid URL: " + urlStr);
      return null;
    }

    
    
  }
  
  @Override
  public String usage() {
	  ////
  }
  
  @Override
  public void go() {
    SolrServer server = new HttpSolrServer("http://localhost:8983/solr");

    try {
      SolrQuery query = new SolrQuery();
      query.setQuery(querystring);
      query.setFields(StringUtils.join(fieldlist, ","));
      for (String name : params.keySet()) {
        query.add(name, params.get(name));
      }
      QueryResponse response = server.query(query);
      if (!omitHeader) {
        printHeader(query, response);
      }
      query.add("fl", "id");
        for (SolrDocument doc : response.getResults()) {
          List<String>values = new ArrayList<String>();
          for (String fieldname : parseFieldList(query.get("fl"))) {
            values.add(toString(doc.getFieldValue(fieldname)));
          }
            System.out.println(StringUtils.join(values, "\t"));
        }
    } catch (SolrServerException e) {
      throw new RuntimeException(e);
    }
  }

  private void printHeader(SolrQuery query, QueryResponse response) {
    long num = response.getResults().getNumFound();
    System.out.println("count: " + num);
      List<String>values = new ArrayList<String>();
      for (String fieldname : parseFieldList(query.get("fl"))) {
        values.add(fieldname);
      }
      System.out.println(StringUtils.join(values, "\t"));
  }

  private List<String> parseFieldList(String fieldlist) {
    List<String> fieldnames = new ArrayList<String>();
    for (String fieldname : fieldlist.split(",")) {
      fieldnames.add(fieldname.trim());
    }
    return fieldnames;
  }
  private String toString(Object obj) {
    if (obj instanceof String) {
      return (String)obj;
    } else if (obj == null) {
      return "";
    } else {
      throw new RuntimeException("Unknown object type: " + obj.getClass().getName());
    }
  }
  
    private static final String DEFAULT_POST_URL = "http://localhost:8983/solr/update";
  
    private static final String DEFAULT_COMMIT = "yes";
    private static final String DEFAULT_OPTIMIZE = "no";
    private static final String DEFAULT_OUT = "no";
    private static final String DEFAULT_AUTO = "no";
    private static final String DEFAULT_RECURSIVE = "0";
    private static final int DEFAULT_WEB_DELAY = 10;
    private static final int MAX_WEB_DEPTH = 10;
    private static final String DEFAULT_CONTENT_TYPE = "application/xml";
    private static final String DEFAULT_FILE_TYPES = "xml,json,csv,pdf,doc,docx,ppt,pptx,xls,xlsx,odt,odp,ods,ott,otp,ots,rtf,htm,html,txt,log"; 

    // Input args
    boolean auto = false;
    int recursive = 0;
    int delay = 0;
    String fileTypes;
    URL solrUrl;
    OutputStream out = null;
    String type;
    String mode;

    private int currentDepth;

    static HashMap<String,String> mimeMap;
    GlobFileFilter globFileFilter;
    // Backlog for crawling
    List<LinkedHashSet<URL>> backlog = new ArrayList<LinkedHashSet<URL>>();
    Set<URL> visited = new HashSet<URL>();
    
    static final String USAGE_STRING_SHORT =
        "Usage: java [SystemProperties] -jar post.jar [-h|-] [<file|folder|url|arg> [<file|folder|url|arg>...]]";

    // Used in tests to avoid doing actual network traffic
    static boolean mockMode = false;
    static PageFetcher pageFetcher;

    static {
      mimeMap = new HashMap<String,String>();
      mimeMap.put("xml", "text/xml");
      mimeMap.put("csv", "text/csv");
      mimeMap.put("json", "application/json");
      mimeMap.put("pdf", "application/pdf");
      mimeMap.put("rtf", "text/rtf");
      mimeMap.put("html", "text/html");
      mimeMap.put("htm", "text/html");
      mimeMap.put("doc", "application/msword");
      mimeMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
      mimeMap.put("ppt", "application/vnd.ms-powerpoint");
      mimeMap.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
      mimeMap.put("xls", "application/vnd.ms-excel");
      mimeMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      mimeMap.put("odt", "application/vnd.oasis.opendocument.text");
      mimeMap.put("ott", "application/vnd.oasis.opendocument.text");
      mimeMap.put("odp", "application/vnd.oasis.opendocument.presentation");
      mimeMap.put("otp", "application/vnd.oasis.opendocument.presentation");
      mimeMap.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
      mimeMap.put("ots", "application/vnd.oasis.opendocument.spreadsheet");
      mimeMap.put("txt", "text/plain");
      mimeMap.put("log", "text/plain");
    }
    
    /**
     * After initialization, call execute to start the post job.
     * This method delegates to the correct mode method.
     */
    public void execute() {
        doWebMode();
      
      if (commit)   commit();
      if (optimize) optimize();
      final long endTime = System.currentTimeMillis();
      displayTiming(endTime - startTime);
    }
    
    private int doWebMode() {
      reset();
      int numPagesPosted = 0;
      try {
        if(type != null) {
          fatal("Specifying content-type with \"-Ddata=web\" is not supported");
        }
        if (arguments.get(0).equals("-")) {
          // Skip posting url if special param "-" given  
          return 0;
        }
        // Set Extracting handler as default
        solrUrl = appendUrlPath(solrUrl, "/extract");
        
        info("Posting web pages to Solr url "+solrUrl);
        auto=true;
        info("Entering auto mode. Indexing pages with content-types corresponding to file endings "+fileTypes);
        if(recursive > 0) {
          if(recursive > MAX_WEB_DEPTH) {
            recursive = MAX_WEB_DEPTH;
            warn("Too large recursion depth for web mode, limiting to "+MAX_WEB_DEPTH+"...");
          }
          if(delay < DEFAULT_WEB_DELAY)
            warn("Never crawl an external web site faster than every 10 seconds, your IP will probably be blocked");
          info("Entering recursive mode, depth="+recursive+", delay="+delay+"s");
        }
        numPagesPosted = postWebPages(arguments, 0, out);
        info(numPagesPosted + " web pages indexed.");
      } catch(MalformedURLException e) {
        fatal("Wrong URL trying to append /extract to "+solrUrl);
      }
      return numPagesPosted;
    }

    private void reset() {
      fileTypes = DEFAULT_FILE_TYPES;
      globFileFilter = this.getFileFilterFromFileTypes(fileTypes);
      backlog = new ArrayList<LinkedHashSet<URL>>();
      visited = new HashSet<URL>();
    }

    /**
     * This method takes as input a list of start URL strings for crawling,
     * adds each one to a backlog and then starts crawling
     * @param args the raw input args from main()
     * @param startIndexInArgs offset for where to start
     * @param out outputStream to write results to
     * @return the number of web pages posted
     */
    public int postWebPages(List<String>args, int startIndexInArgs, OutputStream out) {
      reset();
      LinkedHashSet<URL> s = new LinkedHashSet<URL>();
      for (int j = startIndexInArgs; j < args.length; j++) {
        try {
          URL u = new URL(normalizeUrlEnding(args[j]));
          s.add(u);
        } catch(MalformedURLException e) {
          warn("Skipping malformed input URL: "+args[j]);
        }
      }
      // Add URLs to level 0 of the backlog and start recursive crawling
      backlog.add(s);
      return webCrawl(0, out);
    }

    /**
     * Normalizes a URL string by removing anchor part and trailing slash
     * @return the normalized URL string
     */
    protected static String normalizeUrlEnding(String link) {
      if(link.indexOf("#") > -1)
        link = link.substring(0,link.indexOf("#"));
      if(link.endsWith("?"))
        link = link.substring(0,link.length()-1);
      if(link.endsWith("/"))
        link = link.substring(0,link.length()-1);
      return link;
    }

    /**
     * A very simple crawler, pulling URLs to fetch from a backlog and then
     * recurses N levels deep if recursive>0. Links are parsed from HTML
     * through first getting an XHTML version using SolrCell with extractOnly,
     * and followed if they are local. The crawler pauses for a default delay
     * of 10 seconds between each fetch, this can be configured in the delay
     * variable. This is only meant for test purposes, as it does not respect
     * robots or anything else fancy :)
     * @param level which level to crawl
     * @param out output stream to write to
     * @return number of pages crawled on this level and below
     */
    protected int webCrawl(int level, OutputStream out) {
      int numPages = 0;
      LinkedHashSet<URL> stack = backlog.get(level);
      int rawStackSize = stack.size();
      stack.removeAll(visited);
      int stackSize = stack.size();
      LinkedHashSet<URL> subStack = new LinkedHashSet<URL>();
      info("Entering crawl at level "+level+" ("+rawStackSize+" links total, "+stackSize+" new)");
      for(URL u : stack) {
        try {
          visited.add(u);
          PageFetcherResult result = pageFetcher.readPageFromUrl(u);
          if(result.httpStatus == 200) {
            u = (result.redirectUrl != null) ? result.redirectUrl : u;
            URL postUrl = new URL(appendParam(solrUrl.toString(), 
                "literal.id="+URLEncoder.encode(u.toString(),"UTF-8") +
                "&literal.url="+URLEncoder.encode(u.toString(),"UTF-8")));
            boolean success = postData(new ByteArrayInputStream(result.content), null, out, result.contentType, postUrl);
            if (success) {
              info("POSTed web resource "+u+" (depth: "+level+")");
              Thread.sleep(delay * 1000);
              numPages++;
              // Pull links from HTML pages only
              if(recursive > level && result.contentType.equals("text/html")) {
                Set<URL> children = pageFetcher.getLinksFromWebPage(u, new ByteArrayInputStream(result.content), result.contentType, postUrl);
                subStack.addAll(children);
              }
            } else {
              warn("An error occurred while posting "+u);
            }
          } else {
            warn("The URL "+u+" returned a HTTP result status of "+result.httpStatus);
          }
        } catch (IOException e) {
          warn("Caught exception when trying to open connection to "+u+": "+e.getMessage());
        } catch (InterruptedException e) {
          throw new RuntimeException();
        }
      }
      if(!subStack.isEmpty()) {
        backlog.add(subStack);
        numPages += webCrawl(level+1, out);
      }
      return numPages;    
    }

    /**
     * Reads an input stream into a byte array
     * @param is the input stream
     * @return the byte array
     * @throws IOException If there is a low-level I/O error.
     */
    protected byte[] inputStreamToByteArray(InputStream is) throws IOException {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      int next = is.read();
      while (next > -1) {
          bos.write(next);
          next = is.read();
      }
      bos.flush();
      is.close();
      return bos.toByteArray();
    }

    /**
     * Computes the full URL based on a base url and a possibly relative link found
     * in the href param of an HTML anchor.
     * @param baseUrl the base url from where the link was found
     * @param link the absolute or relative link
     * @return the string version of the full URL
     */
    protected String computeFullUrl(URL baseUrl, String link) {
      if(link == null || link.length() == 0) {
        return null;
      }
      if(!link.startsWith("http")) {
        if(link.startsWith("/")) {
          link = baseUrl.getProtocol() + "://" + baseUrl.getAuthority() + link;
        } else {
          if(link.contains(":")) {
            return null; // Skip non-relative URLs
          }
          String path = baseUrl.getPath();
          if(!path.endsWith("/")) {
            int sep = path.lastIndexOf("/");
            String file = path.substring(sep+1);
            if(file.contains(".") || file.contains("?"))
              path = path.substring(0,sep);
          }
          link = baseUrl.getProtocol() + "://" + baseUrl.getAuthority() + path + "/" + link;
        }
      }
      link = normalizeUrlEnding(link);
      String l = link.toLowerCase(Locale.ROOT);
      // Simple brute force skip images
      if(l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".png") || l.endsWith(".gif")) {
        return null; // Skip images
      }
      return link;
    }

    /**
     * Uses the mime-type map to reverse lookup whether the file ending for our type
     * is supported by the fileTypes option
     * @param type what content-type to lookup
     * @return true if this is a supported content type
     */
    protected boolean typeSupported(String type) {
      for(String key : mimeMap.keySet()) {
        if(mimeMap.get(key).equals(type)) {
          if(fileTypes.contains(key))
            return true;
        }
      }
      return false;
    }

    /**
     * Tests if a string is either "true", "on", "yes" or "1"
     * @param property the string to test
     * @return true if "on"
     */
    protected static boolean isOn(String property) {
      return("true,on,yes,1".indexOf(property) > -1);
    }
    
    /**
     * Opens the file and posts it's contents to the solrUrl,
     * writes to response to output. 
     */
    public void postFile(File file, OutputStream output, String type) {
      InputStream is = null;
      try {
        URL url = solrUrl;
        if(auto) {
          if(type == null) {
            type = guessType(file);
          }
          if(type != null) {
            if(type.equals("text/xml") || type.equals("text/csv") || type.equals("application/json")) {
              // Default handler
            } else {
              // SolrCell
              String urlStr = appendUrlPath(solrUrl, "/extract").toString();
              if(urlStr.indexOf("resource.name")==-1)
                urlStr = appendParam(urlStr, "resource.name=" + URLEncoder.encode(file.getAbsolutePath(), "UTF-8"));
              if(urlStr.indexOf("literal.id")==-1)
                urlStr = appendParam(urlStr, "literal.id=" + URLEncoder.encode(file.getAbsolutePath(), "UTF-8"));
              url = new URL(urlStr);
            }
          } else {
            warn("Skipping "+file.getName()+". Unsupported file type for auto mode.");
            return;
          }
        } else {
          if(type == null) type = DEFAULT_CONTENT_TYPE;
        }
        info("POSTing file " + file.getName() + (auto?" ("+type+")":""));
        is = new FileInputStream(file);
        postData(is, (int)file.length(), output, type, url);
      } catch (IOException e) {
        e.printStackTrace();
        warn("Can't open/read file: " + file);
      } finally {
        try {
          if(is!=null) is.close();
        } catch (IOException e) {
          fatal("IOException while closing file: "+ e);
        }
      }
    }

    /**
     * Appends to the path of the URL
     * @param url the URL
     * @param append the path to append
     * @return the final URL version 
     */
    protected static URL appendUrlPath(URL url, String append) throws MalformedURLException {
      return new URL(url.getProtocol() + "://" + url.getAuthority() + url.getPath() + append + (url.getQuery() != null ? "?"+url.getQuery() : ""));
    }

    /**
     * Guesses the type of a file, based on file name suffix
     * @param file the file
     * @return the content-type guessed
     */
    protected static String guessType(File file) {
      String name = file.getName();
      String suffix = name.substring(name.lastIndexOf(".")+1);
      return mimeMap.get(suffix.toLowerCase(Locale.ROOT));
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
     * Converts a string to an input stream 
     * @param s the string
     * @return the input stream
     */
    public static InputStream stringToStream(String s) {
      InputStream is = null;
      try {
        is = new ByteArrayInputStream(s.getBytes("UTF-8"));
      } catch (UnsupportedEncodingException e) {
        fatal("Shouldn't happen: UTF-8 not supported?!?!?!");
      }
      return is;
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

    public GlobFileFilter getFileFilterFromFileTypes(String fileTypes) {
      String glob;
      if(fileTypes.equals("*"))
        glob = ".*";
      else
        glob = "^.*\\.(" + fileTypes.replace(",", "|") + ")$";
      return new GlobFileFilter(glob, true);
    }

    //
    // Utility methods for XPath handing
    //
    
    /**
     * Gets all nodes matching an XPath
     */
    public static NodeList getNodesFromXP(Node n, String xpath) throws XPathExpressionException {
      XPathFactory factory = XPathFactory.newInstance();
      XPath xp = factory.newXPath();
      XPathExpression expr = xp.compile(xpath);
      return (NodeList) expr.evaluate(n, XPathConstants.NODESET);
    }
    
    /**
     * Gets the string content of the matching an XPath
     * @param n the node (or doc)
     * @param xpath the xpath string
     * @param concatAll if true, text from all matching nodes will be concatenated, else only the first returned
     */
    public static String getXP(Node n, String xpath, boolean concatAll)
        throws XPathExpressionException {
      NodeList nodes = getNodesFromXP(n, xpath);
      StringBuffer sb = new StringBuffer();
      if (nodes.getLength() > 0) {
        for(int i = 0; i < nodes.getLength() ; i++) {
          sb.append(nodes.item(i).getNodeValue() + " ");
          if(!concatAll) break;
        }
        return sb.toString().trim();
      } else
        return "";
    }
    
    /**
     * Takes a string as input and returns a DOM 
     */
    public static Document makeDom(String in, String inputEncoding) throws SAXException, IOException,
    ParserConfigurationException {
      InputStream is = new ByteArrayInputStream(in
          .getBytes(inputEncoding));
      Document dom = DocumentBuilderFactory.newInstance()
          .newDocumentBuilder().parse(is);
      return dom;
    }

    /**
     * Inner class to filter files based on glob wildcards
     */
    class GlobFileFilter implements FileFilter
    {
      private String _pattern;
      private Pattern p;
      
      public GlobFileFilter(String pattern, boolean isRegex)
      {
        _pattern = pattern;
        if(!isRegex) {
          _pattern = _pattern
              .replace("^", "\\^")
              .replace("$", "\\$")
              .replace(".", "\\.")
              .replace("(", "\\(")
              .replace(")", "\\)")
              .replace("+", "\\+")
              .replace("*", ".*")
              .replace("?", ".");
          _pattern = "^" + _pattern + "$";
        }
        
        try {
          p = Pattern.compile(_pattern,Pattern.CASE_INSENSITIVE);
        } catch(PatternSyntaxException e) {
          fatal("Invalid type list "+pattern+". "+e.getDescription());
        }
      }
      
      @Override
      public boolean accept(File file)
      {
        return p.matcher(file.getName()).find();
      }
    }
    
    //
    // Simple crawler class which can fetch a page and check for robots.txt
    //
    class PageFetcher {
      Map<String, List<String>> robotsCache;
      final String DISALLOW = "Disallow:";
      
      public PageFetcher() {
        robotsCache = new HashMap<String,List<String>>();
      }
      
      public PageFetcherResult readPageFromUrl(URL u) {
        PageFetcherResult res = new PageFetcherResult();
        try {
          if (isDisallowedByRobots(u)) {
            warn("The URL "+u+" is disallowed by robots.txt and will not be crawled.");
            res.httpStatus = 403;
            visited.add(u);
            return res;
          }
          res.httpStatus = 404;
          HttpURLConnection conn = (HttpURLConnection) u.openConnection();
          conn.setRequestProperty("User-Agent", "SimplePostTool-crawler/"+VERSION_OF_THIS_TOOL+" (http://lucene.apache.org/solr/)");
          conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
          conn.connect();
          res.httpStatus = conn.getResponseCode();
          if(!normalizeUrlEnding(conn.getURL().toString()).equals(normalizeUrlEnding(u.toString()))) {
            info("The URL "+u+" caused a redirect to "+conn.getURL());
            u = conn.getURL();
            res.redirectUrl = u;
            visited.add(u);
          }
          if(res.httpStatus == 200) {
            // Raw content type of form "text/html; encoding=utf-8"
            String rawContentType = conn.getContentType();
            String type = rawContentType.split(";")[0];
            if(typeSupported(type)) {
              String encoding = conn.getContentEncoding();
              InputStream is;
              if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                is = new GZIPInputStream(conn.getInputStream());
              } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
                is = new InflaterInputStream(conn.getInputStream(), new Inflater(true));
              } else {
                is = conn.getInputStream();
              }
              
              // Read into memory, so that we later can pull links from the page without re-fetching 
              res.content = inputStreamToByteArray(is);
              is.close();
            } else {
              warn("Skipping URL with unsupported type "+type);
              res.httpStatus = 415;
            }
          }
        } catch(IOException e) {
          warn("IOException when reading page from url "+u+": "+e.getMessage());
        }
        return res;
      }
      
      public boolean isDisallowedByRobots(URL url) {
        String host = url.getHost();
        String strRobot = url.getProtocol() + "://" + host + "/robots.txt";
        List<String> disallows = robotsCache.get(host);
        if(disallows == null) {
          disallows = new ArrayList<String>();
          URL urlRobot;
          try { 
            urlRobot = new URL(strRobot);
            disallows = parseRobotsTxt(urlRobot.openStream());
          } catch (MalformedURLException e) {
            return true; // We cannot trust this robots URL, should not happen
          } catch (IOException e) {
            // There is no robots.txt, will cache an empty disallow list
          }
        }
        
        robotsCache.put(host, disallows);

        String strURL = url.getFile();
        for (String path : disallows) {
          if (path.equals("/") || strURL.indexOf(path) == 0)
            return true;
        }
        return false;
      }

      /**
       * Very simple robots.txt parser which obeys all Disallow lines regardless
       * of user agent or whether there are valid Allow: lines.
       * @param is Input stream of the robots.txt file
       * @return a list of disallow paths
       * @throws IOException if problems reading the stream
       */
      protected List<String> parseRobotsTxt(InputStream is) throws IOException {
        List<String> disallows = new ArrayList<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String l;
        while((l = r.readLine()) != null) {
          String[] arr = l.split("#");
          if(arr.length == 0) continue;
          l = arr[0].trim();
          if(l.startsWith(DISALLOW)) {
            l = l.substring(DISALLOW.length()).trim();
            if(l.length() == 0) continue;
            disallows.add(l);
          }
        }
        is.close();
        return disallows;
      }

      /**
       * Finds links on a web page, using /extract?extractOnly=true
       * @param u the URL of the web page
       * @param is the input stream of the page
       * @param type the content-type
       * @param postUrl the URL (typically /solr/extract) in order to pull out links
       * @return a set of URLs parsed from the page
       */
      protected Set<URL> getLinksFromWebPage(URL u, InputStream is, String type, URL postUrl) {
        Set<URL> l = new HashSet<URL>();
        URL url = null;
        try {
          ByteArrayOutputStream os = new ByteArrayOutputStream();
          URL extractUrl = new URL(appendParam(postUrl.toString(), "extractOnly=true"));
          boolean success = postData(is, null, os, type, extractUrl);
          if(success) {
            String rawXml = os.toString("UTF-8");
            Document d = makeDom(rawXml, "UTF-8");
            String innerXml = getXP(d, "/response/str/text()[1]", false);
            d = makeDom(innerXml, "UTF-8");
            NodeList links = getNodesFromXP(d, "/html/body//a/@href");
            for(int i = 0; i < links.getLength(); i++) {
              String link = links.item(i).getTextContent();
              link = computeFullUrl(u, link);
              if(link == null)
                continue;
              url = new URL(link);
              if(url.getAuthority() == null || !url.getAuthority().equals(u.getAuthority()))
                continue;
              l.add(url);
            }
          }
        } catch (MalformedURLException e) {
          warn("Malformed URL "+url);
        } catch (IOException e) {
          warn("IOException opening URL "+url+": "+e.getMessage());
        } catch (Exception e) {
          throw new RuntimeException();
        }
        return l;
      }
    }
      
    /**
     * Utility class to hold the result form a page fetch
     */
    public class PageFetcherResult {
      int httpStatus = 200;
      String contentType = "text/html";
      URL redirectUrl = null;
      byte[] content;
    }
  }

}
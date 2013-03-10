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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class FilePostAction extends AbstractPostAction {

  private static final String DEFAULT_CONTENT_TYPE = "application/xml";
  //private static final String DEFAULT_FILE_TYPES = "xml,json,csv,pdf,doc,docx,ppt,pptx,xls,xlsx,odt,odp,ods,ott,otp,ots,rtf,htm,html,txt,log"; 

  private List<String> files = null;
  private Map<String,String> params = new HashMap<String, String>();
  private boolean auto = false;
  private int recursive = 0;
  private int delay = 0;
  private String fileTypes;
  private OutputStream out = null;
  private String type;
  
  private int currentDepth;

  static HashMap<String,String> mimeMap = new HashMap<String,String>();
  GlobFileFilter globFileFilter;
  
  static {
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
      
  
  @Override
  public void init() {
    for (Entry<String, String> option : options.entrySet()) {
        String key = option.getKey();
        String value = option.getValue();
        if (super.checkParam(key, value)) {
      	  continue;
        } else if (key.equals("recursive")) {
        	recursive = parseRecursive(value);
        } else if (key.equals("delay")) {
        	delay = Integer.parseInt(value);
        } else {
          params.put(key,  value);
        }
     }
    
    files = arguments;
  }

  private int parseRecursive(String recursiveStr) {
      try {
        return Integer.parseInt(recursiveStr);
      } catch(Exception e) {
        if (isTrue(recursiveStr)) {
          return 999;
        } else {
    	  return 0;
        }
      }
  }

  @Override
  public String usage() {
	  return "";
  }
  
  @Override
  public void go() {
    currentDepth = 0;
    info("Posting files to base url " + solrUrl + (!auto?" using content-type "+(type==null?DEFAULT_CONTENT_TYPE:type):"")+"..");
    if(auto)
      info("Entering auto mode. File endings considered are "+fileTypes);
    if(recursive > 0)
      info("Entering recursive mode, max depth="+recursive+", delay="+delay+"s"); 
    int numFilesPosted = postFiles(files, out, type);
    info(numFilesPosted + " files indexed.");

    if (commit)   commit();
    if (optimize) optimize();
  }
  

    /** Post all filenames provided in srcFiles
     * @param srcFiles array of file names
     * @param out output stream to post data to
     * @param type default content-type to use when posting (may be overridden in auto mode)
     * @return number of files posted
     * */
    private int postFiles(List <String> srcFiles, OutputStream out, String type) {
      int filesPosted = 0;
      for (String file: srcFiles) {
        File srcFile = new File(file);
        if(srcFile.isDirectory() && srcFile.canRead()) {
          filesPosted += postDirectory(srcFile, out, type);
        } else if (srcFile.isFile() && srcFile.canRead()) {
          filesPosted += postFiles(new File[] {srcFile}, out, type);
        } else {
          File parent = srcFile.getParentFile();
          if(parent == null) parent = new File(".");
          String fileGlob = srcFile.getName();
          GlobFileFilter ff = new GlobFileFilter(fileGlob, false);
          File[] files = parent.listFiles(ff);
          if(files == null || files.length == 0) {
            warn("No files or directories matching "+srcFile);
            continue;          
          }
          filesPosted += postFiles(parent.listFiles(ff), out, type);
        }
      }
      return filesPosted;
    }
    
    /**
     * Posts a whole directory
     * @return number of files posted total
     */
    private int postDirectory(File dir, OutputStream out, String type) {
      if(dir.isHidden() && !dir.getName().equals("."))
        return(0);
      info("Indexing directory "+dir.getPath()+" ("+dir.listFiles(globFileFilter).length+" files, depth="+currentDepth+")");
      int posted = 0;
      posted += postFiles(dir.listFiles(globFileFilter), out, type);
      if(recursive > currentDepth) {
        for(File d : dir.listFiles()) {
          if(d.isDirectory()) {
            currentDepth++;
            posted += postDirectory(d, out, type);
            currentDepth--;
          }
        }
      }
      return posted;
    }

    /**
     * Posts a list of file names
     * @return number of files posted
     */
    private int postFiles(File[] files, OutputStream out, String type) {
      int filesPosted = 0;
      for(File srcFile : files) {
        try {
          if(!srcFile.isFile() || srcFile.isHidden())
            continue;
          postFile(srcFile, out, type);
          Thread.sleep(delay * 1000);
          filesPosted++;
        } catch (InterruptedException e) {
          throw new RuntimeException();
        }
      }
      return filesPosted;
    }

    /**
     * Opens the file and posts it's contents to the solrUrl,
     * writes to response to output. 
     */
    private void postFile(File file, OutputStream output, String type) {
      InputStream is = null;
      try {
        URL url = getSolrUpdateURL();
        if(auto) {
          if(type == null) {
            type = guessType(file);
          }
          if(type != null) {
            if(type.equals("text/xml") || type.equals("text/csv") || type.equals("application/json")) {
              // Default handler
            } else {
              // SolrCell
              String urlStr = appendUrlPath(url, "/extract").toString();
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
     * Guesses the type of a file, based on file name suffix
     * @param file the file
     * @return the content-type guessed
     */
    private static String guessType(File file) {
      String name = file.getName();
      String suffix = name.substring(name.lastIndexOf(".")+1);
      return mimeMap.get(suffix.toLowerCase(Locale.ROOT));
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
    

}

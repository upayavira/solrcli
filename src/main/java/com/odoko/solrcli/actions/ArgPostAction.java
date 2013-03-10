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
import java.util.Map;
import java.util.Map.Entry;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

public class ArgPostAction extends AbstractPostAction {

  private Map<String,String> params = new HashMap<String, String>();
  private OutputStream out = null;

  @Override
  public String usage() {
	return "-commit -optimize -verbose -url=http://localhost:8983/solr/update";
  }

  //VERBOSE is needed
  @Override
  public void init() {
    for (Entry<String, String> option : options.entrySet()) {
      String key = option.getKey();
      String value = option.getValue();
      if (super.checkParam(key, value)) {
    	  continue;
      } else {
        params.put(key,  value);
      }
    }
  }

  @Override
  public void go() {
    info("POSTing stdin to " + solrUrl + "..");
    for (String arg : arguments) {
      postData(stringToStream(arg), null, out, null, getSolrURL());
    }
    if (commit)   commit();
    if (optimize) optimize();
  }

	/**
	 * Converts a string to an input stream 
	 * @param s the string
	 * @return the input stream
	 */
	public InputStream stringToStream(String s) {
	  InputStream is = null;
	  try {
	    is = new ByteArrayInputStream(s.getBytes("UTF-8"));
	  } catch (UnsupportedEncodingException e) {
	    fatal("Shouldn't happen: UTF-8 not supported?!?!?!");
	  }
	  return is;
	}
	
}

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

import java.io.OutputStream;


public class StdinPostAction extends AbstractPostAction {

  private Map<String,String> params = new HashMap<String, String>();
  private OutputStream out = null;

  @Override
  public String usage() {
	return "-commit -optimize -verbose -url=http://localhost:8983/solr/update";
  }

  //VERBOSE is needed
  @Override
  public void init() {
    if (arguments.size() > 0) {
      throw new RuntimeException("Not expecting any arguments");
    }
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
    postData(System.in, null, out, null, getSolrURL());    
    if (commit)   commit();
    if (optimize) optimize();
  }
}

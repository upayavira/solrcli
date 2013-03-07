package com.odoko;

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

public class SearchAction extends AbstractAction {

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
}
